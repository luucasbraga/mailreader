package br.com.groupsoftware.grouppay.extratoremail.job;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.ClientGroup;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.Status;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import br.com.groupsoftware.grouppay.extratoremail.service.ServiceFacade;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Job responsável pelo processamento de e-mails para múltiplas empresas.
 * Utiliza um pool de threads para realizar o processamento em paralelo, garantindo eficiência no tratamento de múltiplos e-mails.
 * O job é executado automaticamente pelo Quartz e controla o status de processamento para evitar sobrecarga no sistema.
 *
 * <p>Durante a execução, cada e-mail é processado individualmente, e o status de cada empresa é atualizado para
 * indicar que está em processamento ou que foi concluído. Se ocorrer um erro, ele será capturado e logado.</p>
 *
 * <p>A classe implementa um método de shutdown para garantir que o executor de threads seja desligado
 * de forma segura ao encerrar a aplicação.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailProcessJob implements Job {

    @Value("${reader.max-emails}")
    private int maxEmails;

    @Value("${email-processing-retry-delay}")
    private int emailProcessingRetryDelay;

    @Value("${reader.dir}")
    private String readerDir;

    private final ServiceFacade service;
    private final RepositoryFacade repository;
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Override
    public void execute(JobExecutionContext context) {
        log.info("[EMAIL_PROCESSING] Iniciando o processamento de e-mails por ClientGroup.");

        int qtdDatabase = repository.clientGroup.findAll().size();
        int qtdProcessing = repository.clientGroup.countAllByStatusProcessing();

        if (qtdProcessing >= maxEmails || qtdDatabase == 0) {
            log.info("Limite de emails processados atingido ou nenhum email disponível.");
            return;
        }

        int maxProcess = maxEmails - qtdProcessing;

        try {
            Pageable pageable = PageRequest.of(0, maxProcess);
            LocalDateTime minDateTime = LocalDateTime.now().minusMinutes(emailProcessingRetryDelay);

            // Buscar ClientGroups elegíveis para processamento
            List<ClientGroup> clientGroupsToProcess = repository.clientGroup
                    .findClientGroupsEligibleForEmailProcessing(minDateTime, pageable).getContent();

            if (!clientGroupsToProcess.isEmpty()) {
                log.info("[EMAIL_PROCESSING] Encontrados {} ClientGroups para processar.", clientGroupsToProcess.size());

                clientGroupsToProcess.parallelStream().forEach(clientGroup ->
                        executorService.submit(() -> {
                            try {
                                clientGroup.setStatus(Status.PROCESSING);
                                repository.clientGroup.save(clientGroup);

                                service.email.getEmailsAndSavePdfs(clientGroup);

                            } catch (Exception e) {
                                log.error("[EMAIL_PROCESS_ERROR] Erro ao processar e-mails para o ClientGroup {}: {}",
                                        clientGroup.getId(), e.getMessage());
                            } finally {
                                clientGroup.setStatus(Status.NOT_PROCESSING);
                                repository.clientGroup.save(clientGroup);
                            }
                        }));
            } else {
                log.info("[EMAIL_PROCESSING] Nenhum ClientGroup encontrado para processar.");
            }
        } finally {
            log.info("[EMAIL_PROCESSING] Processamento de e-mails concluído.");
        }
    }

    /**
     * Método chamado antes da destruição do bean para garantir o desligamento seguro do {@link ExecutorService}.
     * O método tenta encerrar as threads em execução e força o encerramento caso não seja possível dentro do tempo limite.
     */
    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}
