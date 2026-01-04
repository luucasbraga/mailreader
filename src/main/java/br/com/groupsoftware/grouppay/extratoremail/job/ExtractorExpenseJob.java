package br.com.groupsoftware.grouppay.extratoremail.job;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentStage;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.Status;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import br.com.groupsoftware.grouppay.extratoremail.service.ServiceFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Job responsável por extrair dados de documentos PDF cujas senhas já foram removidas.
 * O job percorre os documentos que estão no estagio {@link DocumentStage#PASSWORD_REMOVED} e
 * utiliza o serviço para extrair as informações e convertê-las em uma instância de {@link Expense}.
 *
 * <p>Após a extração, a instância de {@link Expense} é convertida em JSON e armazenada no
 * próprio documento. O estagio do documento é então atualizado para {@link DocumentStage#EXPENSE_EXTRACTED}.</p>
 *
 * <p>Os logs detalham o processo, incluindo o número de documentos processados e quaisquer
 * erros que possam ocorrer durante a extração.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractorExpenseJob implements Job {

    @Value("${document-processing-retry-delay}")
    private int documentProcessingRetryDelay;

    private final RepositoryFacade repository;
    private final ServiceFacade service;
    private final ObjectMapper objectMapper;

    @Override
    public void execute(JobExecutionContext context) {
        log.info("[EXPENSE_EXTRACTION] Iniciando o job de extração de dados dos documentos.");

        LocalDateTime minDateTime = LocalDateTime.now().minusMinutes(documentProcessingRetryDelay);
        List<Document> documentsToProcess = repository.document
                .findDocumentsEligibleForProcessing(DocumentStage.TEXT_EXTRACTED, minDateTime);

        if (!documentsToProcess.isEmpty()) {
            log.info("Extraindo dados de {} documentos.", documentsToProcess.size());

            documentsToProcess.parallelStream().forEach(document -> {
                try {
                    service.document.changeStatus(document, Status.PROCESSING);

                    Expense expense = service.pdf.getExpenseFromDocument(document);
                    if (expense != null) {
                        expense.setCompanyUUID(document.getClientGroup().getUuid()); 
                        String despesaJson = objectMapper.writeValueAsString(expense);
                        document.setExpenseJson(despesaJson);
                        service.document.changeStage(document, DocumentStage.EXPENSE_EXTRACTED);
                    }
                } catch (Exception e) {
                    log.error("Erro ao extrair dados do documento: {}", document.getFileName(), e);
                    service.document.changeStage(document, DocumentStage.ERRO);
                } finally {
                    service.document.changeStatus(document, Status.NOT_PROCESSING);
                }
            });
        } else {
            log.info("Nenhum PDF encontrado para extração de dados.");
        }
    }
}