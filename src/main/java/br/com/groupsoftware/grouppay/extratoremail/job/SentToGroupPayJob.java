package br.com.groupsoftware.grouppay.extratoremail.job;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentStage;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.Status;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.ExpenseDTO;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import br.com.groupsoftware.grouppay.extratoremail.service.ServiceFacade;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Job responsável pelo envio de despesas para o serviço GroupPay.
 *
 * <p>Este job é executado periodicamente e processa documentos com o estágio
 * {@link DocumentStage#EXPENSE_EXTRACTED}, transformando-os em objetos
 * {@link ExpenseDTO} e enviando-os para o serviço GroupPay através do
 * {@link ServiceFacade}.</p>
 *
 * <p>Os logs são utilizados para rastrear o progresso e tratar possíveis erros
 * durante o processamento dos documentos.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class SentToGroupPayJob implements Job {

    @Value("${document-processing-retry-delay}")
    private int documentProcessingRetryDelay;

    private final RepositoryFacade repository;
    private final ServiceFacade service;
    private final ObjectMapper objectMapper;

    @Override
    public void execute(JobExecutionContext context) {
        log.info("[GROUPPAY_SEND] Iniciando o job de envio de despesas.");
        LocalDateTime minDateTime = LocalDateTime.now().minusMinutes(documentProcessingRetryDelay);
        List<Document> documentsToProcess = repository.document.findDocumentsEligibleForProcessing(DocumentStage.COMPANY_MATCHED, minDateTime);

        if (documentsToProcess.isEmpty()) {
            log.info("Nenhum documento encontrado para envio.");
            return;
        }
        log.info("Processando {} documentos para envio.", documentsToProcess.size());

        documentsToProcess.forEach(document -> {
            try {
                service.document.changeStatus(document, Status.PROCESSING);
                processDocument(document);
            } catch (Exception e) {
                log.error("Erro ao processar documento ID: {}", document.getId(), e);
            } finally {
                service.document.changeStatus(document, Status.NOT_PROCESSING);
            }
        });
        log.info("Job concluído.");
    }

    private void processDocument(Document document) throws JsonProcessingException {
        Expense expense = service.expenseMapper.getExpense(document);
        ExpenseDTO expenseDTO = ExpenseDTO.builder()
                .documentId(document.getId())
                .fileName(getFileName(document.getFileName()))
                .codSupport(document.getCompany().getClientGroup().getCodigoSuporte())
                .type(document.getExpenseType())
                .json(objectMapper.writeValueAsString(expense))
                .build();

        service.expenseSender.sendExpense(document, expenseDTO);
    }

    private String getFileName(String originalFileName) {
        int dotIndex = originalFileName.lastIndexOf(".");
        String baseName;
        String extension;

        // Keep extension at the end of name
        if (dotIndex != -1) {
            baseName = originalFileName.substring(0, dotIndex);
            extension = originalFileName.substring(dotIndex);
        } else {
            baseName = originalFileName;
            extension = "";
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return baseName + "_" + timestamp + extension;
    }
}
