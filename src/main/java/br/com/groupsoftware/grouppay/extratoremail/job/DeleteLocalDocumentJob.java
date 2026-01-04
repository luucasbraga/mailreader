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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Job responsável por limpar arquivos da pasta local que foram marcados com o estagio {@link DocumentStage#DELETE_FROM_LOCAL}.
 * Esta classe garante que os arquivos processados sejam removidos do sistema de arquivos local
 * após o término de seu processamento.
 *
 * <p>O job é executado automaticamente pelo Quartz e faz parte do processo de gerenciamento de arquivos
 * no sistema, garantindo que a pasta local não fique sobrecarregada com arquivos desnecessários.</p>
 *
 * <p>Se um arquivo for excluído com sucesso, o estagio do documento é atualizado para {@link DocumentStage#PROCESSED}
 * e um histórico de estagio é adicionado.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteLocalDocumentJob implements Job {

    @Value("${document-processing-retry-delay}")
    private int documentProcessingRetryDelay;
    private final ServiceFacade service;
    private final RepositoryFacade repository;

    @Value("${reader.dir}")
    private String readerDir;

    @Override
    public void execute(JobExecutionContext context) {
        log.info("[CLEANUP] Iniciando o job de limpeza da pasta local.");

        LocalDateTime minDateTime = LocalDateTime.now().minusMinutes(documentProcessingRetryDelay);
        List<Document> documents = repository.document.findDocumentsEligibleForProcessing(DocumentStage.DELETE_FROM_LOCAL, minDateTime);
        if (!documents.isEmpty()) {
            log.info("Arquivos a serem processados para exclusão: {}", documents.size());
            documents.parallelStream().forEach(document -> {
                Path filePath = Paths.get(readerDir, document.getLocalFilePath());
                try {
                    service.document.changeStatus(document, Status.PROCESSING);
                    if (Files.exists(filePath)) {
                        Files.delete(filePath);
                        service.document.changeStage(document, DocumentStage.PROCESSED);
                        log.info("Arquivo {} excluído com sucesso.", document.getFileName());
                    } else {
                        log.warn("Arquivo não encontrado para exclusão: {}", document.getFileName());
                    }
                } catch (IOException e) {
                    log.error("Erro ao limpar arquivo {} da pasta local: {}", document.getFileName(), e.getMessage());
                } finally {
                    service.document.changeStatus(document, Status.NOT_PROCESSING);
                }
            });
        } else {
            log.info("Nenhum arquivo encontrado para limpeza.");
        }
    }
}
