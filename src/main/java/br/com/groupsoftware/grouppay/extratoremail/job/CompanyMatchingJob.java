package br.com.groupsoftware.grouppay.extratoremail.job;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentStage;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.Status;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import br.com.groupsoftware.grouppay.extratoremail.service.CompanyMatchingService;
import br.com.groupsoftware.grouppay.extratoremail.service.ServiceFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CompanyMatchingJob implements Job {

    private final RepositoryFacade repository;
    private final CompanyMatchingService companyMatchingService;
    private final ServiceFacade service;

    @Value("${document-processing-retry-delay}")
    private int documentProcessingRetryDelay;

    @Override
    public void execute(JobExecutionContext context) {
        log.info("[COMPANY_MATCHING] Iniciando o job de matching de documentos com companies.");

        LocalDateTime minDateTime = LocalDateTime.now().minusMinutes(documentProcessingRetryDelay);
        List<Document> documentsToMatch = repository.document
                .findDocumentsEligibleForProcessing(DocumentStage.EXPENSE_EXTRACTED, minDateTime);

        if (!documentsToMatch.isEmpty()) {
            log.info("Fazendo matching de {} documentos.", documentsToMatch.size());

            documentsToMatch.parallelStream().forEach(document -> {
                try {
                    service.document.changeStatus(document, Status.PROCESSING);
                    companyMatchingService.matchDocumentToCompany(document);
                } catch (Exception e) {
                    log.error("Erro ao fazer matching do documento: {}", document.getFileName(), e);
                } finally {
                    service.document.changeStatus(document, Status.NOT_PROCESSING);
                }
            });
        } else {
            log.info("Nenhum documento encontrado para matching.");
        }
    }
}