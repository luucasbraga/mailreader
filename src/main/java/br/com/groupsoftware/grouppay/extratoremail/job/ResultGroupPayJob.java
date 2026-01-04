package br.com.groupsoftware.grouppay.extratoremail.job;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentStage;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.Status;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.DocumentDTO;
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
import java.util.concurrent.ExecutorService;

/**
 * Job responsável pelo processamento de pastas no S3 para múltiplas empresas.
 *
 * <p>Este job busca e processa arquivos armazenados no Amazon S3 em pastas
 * específicas para cada empresa. O processamento é feito de forma paralela,
 * utilizando um pool de threads para garantir eficiência. Durante a execução,
 * o status das empresas é atualizado para evitar conflitos de processamento.</p>
 *
 * <p>Além disso, a classe gerencia o encerramento seguro do {@link ExecutorService}
 * para evitar interrupções no processamento ao desligar a aplicação.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResultGroupPayJob implements Job {

    @Value("${document-processing-retry-delay}")
    private int documentProcessingRetryDelay;

    private final RepositoryFacade repository;
    private final ServiceFacade service;

    @Override
    public void execute(JobExecutionContext context) {
        log.info("[GROUPPAY_RESULTS] Iniciando o job de resultados de processamento group pay.");

        LocalDateTime minDateTime = LocalDateTime.now().minusMinutes(documentProcessingRetryDelay);
        List<Document> documentsToProcess = repository.document.findDocumentsEligibleForProcessing(DocumentStage.SENT_TO_GROUP_PAY, minDateTime);
        if (!documentsToProcess.isEmpty()) {
            log.info("Processando ({}) documentos.", documentsToProcess.size());
            documentsToProcess.parallelStream().forEach(document -> {
                try {
                    service.document.changeStatus(document, Status.PROCESSING);
                    DocumentDTO documentDTO = service.expenseSender.getResultExpense(document);
                    if (documentDTO != null) {
                        service.groupPay.processExpense(documentDTO);
                    }
                } catch (Exception e) {
                    log.error("Erro ao obter resposta do group pay: {}. Erro: {}", document.getFileName(), e.getMessage(), e);
                } finally {
                    service.document.changeStatus(document, Status.NOT_PROCESSING);
                }
            });
        } else {
            log.info("Nenhum documento encontrado para obter resposta do group pay.");
        }
    }
}
