package br.com.groupsoftware.grouppay.extratoremail.job;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Company;
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

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
public class S3DownloadJob implements Job {

    @Value("${reader.max-emails}")
    private int maxEmails;

    @Value("${reader.dir}")
    private String readerDir;

    @Value("${email-processing-retry-delay}")
    private int emailProcessingRetryDelay;

    private final ServiceFacade service;
    private final RepositoryFacade repository;
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Override
    public void execute(JobExecutionContext context) {
        log.info("[S3_PROCESSING] Iniciando o processamento de pastas no S3.");
        int qtdProcessing = repository.company.findAllByStatusProcessing().size();

        if (qtdProcessing >= maxEmails) {
            log.info("Limite de pastas processadas atingido. Nenhuma pasta será processada.");
            return;
        }

        int maxProcess = maxEmails - qtdProcessing;
        int pageSize = 10;
        int pageNumber = 0;
        int totalProcessed = 0;

        try {
            LocalDateTime minDateTime = LocalDateTime.now().minusMinutes(emailProcessingRetryDelay);

            while (totalProcessed < maxProcess) {
                Pageable pageable = PageRequest.of(pageNumber, pageSize);
                List<Company> foldersToProcess = repository.company.findCompaniesEligibleForS3Processing(minDateTime, pageable).getContent();

                if (foldersToProcess.isEmpty()) {
                    log.info("Nenhuma company encontrada para processar na página {}.", pageNumber);
                    break;
                }

                log.info("Processando {} pastas na página {}.", foldersToProcess.size(), pageNumber);

                for (Company company : foldersToProcess) {
                    if (totalProcessed >= maxProcess) {
                        break;
                    }

                    executorService.submit(() -> {
                        try {
                            company.setLocalPath(Paths.get(readerDir, company.getEmail()).toString());
                            company.setStatus(Status.PROCESSING);
                            repository.company.save(company);
                            service.s3Download.downloadFiles(company);
                        } catch (Exception e) {
                            log.error("[S3_DOWNLOAD_ERROR] Erro ao processar arquivos para a company {}: {}", company.getId(), e.getMessage());
                        }
                    });

                    totalProcessed++;
                }

                pageNumber++;
            }
        } finally {
            log.info("[S3_PROCESSING] Processamento de pastas concluído. Total processado: {}", totalProcessed);
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
