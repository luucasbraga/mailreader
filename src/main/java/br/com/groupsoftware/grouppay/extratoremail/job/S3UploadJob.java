package br.com.groupsoftware.grouppay.extratoremail.job;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentStage;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.Status;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import br.com.groupsoftware.grouppay.extratoremail.service.ServiceFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Job responsável por enviar documentos para o armazenamento na AWS S3.
 *
 * <p>Este job processa documentos no estágio {@link DocumentStage#SENT_TO_S3}, realiza
 * o upload dos arquivos para a S3 e atualiza o estágio do documento para
 * {@link DocumentStage#DELETE_FROM_LOCAL} após um envio bem-sucedido.
 *
 * <p>Logs detalham o número de documentos processados e erros encontrados durante
 * o envio.</p>
 *
 * <p>Este job é executado pelo Quartz Scheduler conforme a configuração de agendamento.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3UploadJob implements Job {

    @Value("${document-processing-retry-delay}")
    private int documentProcessingRetryDelay;

    @Value("${reader.dir}")
    private String readerDir;

    private final RepositoryFacade repository;
    private final ServiceFacade service;

    @Override
    public void execute(JobExecutionContext context) {
        log.info("[S3_UPLOAD] Iniciando o job de envio de documentos para o S3.");

        // Busca documentos prontos para envio à S3
        LocalDateTime minDateTime = LocalDateTime.now().minusMinutes(documentProcessingRetryDelay);
        List<Document> documentsToProcess = repository.document.findDocumentsEligibleForProcessing(DocumentStage.SENT_TO_S3, minDateTime);
        if (!documentsToProcess.isEmpty()) {
            log.info("Enviando {} documentos para o S3.", documentsToProcess.size());
            documentsToProcess.parallelStream().forEach(document -> {
                try {
                    service.document.changeStatus(document, Status.PROCESSING);

                    // Verifica se o arquivo ainda existe antes de tentar upload
                    Path filePath = Paths.get(readerDir, document.getLocalFilePath());
                    if (!Files.exists(filePath)) {
                        log.warn("Arquivo não encontrado para upload, pulando: {}", document.getFileName());
                        return;
                    }

                    String response = service.s3Upload.uploadFile(filePath, document.getAmazonPath());
                    if (response != null && !response.isEmpty()) {
                        // Atualiza o estagio do documento após o envio bem-sucedido
                        service.document.changeStage(document, DocumentStage.DELETE_FROM_LOCAL);
                        log.info("Documento enviado com sucesso: {}", document.getFileName());
                    }
                } catch (Exception e) {
                    log.error("Erro ao enviar o documento para o S3: {}. Erro: {}", document.getFileName(), e.getMessage(), e);
                } finally {
                    service.document.changeStatus(document, Status.NOT_PROCESSING);
                }
            });
        } else {
            log.info("Nenhum documento encontrado para envio ao S3.");
        }
    }
}