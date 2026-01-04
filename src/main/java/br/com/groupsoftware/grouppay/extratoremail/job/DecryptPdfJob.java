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

import java.time.LocalDateTime;
import java.util.List;

/**
 * Job responsável por processar e descriptografar documentos PDF que foram baixados.
 * Esta classe busca documentos com o estagio {@link DocumentStage#DOWNLOADED} e
 * utiliza o serviço de descriptografia para processá-los.
 *
 * <p>O processamento é feito em paralelo para garantir eficiência e minimizar o tempo
 * de execução, mas trata exceções individualmente para cada documento.</p>
 *
 * <p>O job é iniciado automaticamente pelo agendador Quartz e é uma parte importante
 * do pipeline de processamento de documentos no sistema.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DecryptPdfJob implements Job {

    @Value("${document-processing-retry-delay}")
    private int documentProcessingRetryDelay;

    private final RepositoryFacade repository;
    private final ServiceFacade service;

    @Override
    public void execute(JobExecutionContext context) {
        log.info("[PDF_DECRYPT] Iniciando o job de decrypt de pdfs baixados.");

        LocalDateTime minDateTime = LocalDateTime.now().minusMinutes(documentProcessingRetryDelay);
        List<Document> documents = repository.document.findDocumentsEligibleForProcessing(DocumentStage.DOWNLOADED, minDateTime);

        if (!documents.isEmpty()) {
            log.info("Processando {} PDFs.", documents.size());
            documents.parallelStream().forEach(document -> {
                try {
                    service.document.changeStatus(document, Status.PROCESSING);
                    service.decryptPdf.decryptPdf(document);
                } catch (Exception e) {
                    log.error("Erro ao descriptografar PDF: {}", document.getFileName(), e);
                } finally {
                    service.document.changeStatus(document, Status.NOT_PROCESSING);
                }
            });
        } else {
            log.info("Nenhum PDF encontrado para processamento.");
        }
    }
}
