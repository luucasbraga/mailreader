package br.com.groupsoftware.grouppay.extratoremail.job;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.DocumentStageHistory;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentStage;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
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
 * Job responsável por limpar arquivos PDF da pasta de downloads que já foram processados.
 * Esta classe busca documentos que não estejam com estagio específicos e remove seus arquivos
 * correspondentes da pasta de downloads.
 *
 * <p>O job é executado automaticamente pelo Quartz e faz parte do processo de gerenciamento de arquivos
 * no sistema, garantindo que a pasta de downloads não fique sobrecarregada com arquivos desnecessários.</p>
 *
 * <p>Se um arquivo for excluído com sucesso, um novo log de estagio é criado e salvo no histórico,
 * indicando que o arquivo foi removido da pasta de downloads.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteDocumentDownloadJob implements Job {

    @Value("${reader.dir}")
    private String readerDir;

    @Value("${reader.download}")
    private String readerDownload;

    private final RepositoryFacade repository;

    @Override
    public void execute(JobExecutionContext context) {
        log.info("[CLEANUP] Iniciando o job de limpeza da pasta 'baixados'.");

        List<DocumentStage> excludedStages = List.of(
                DocumentStage.PROCESSED,
                DocumentStage.DELETED_FROM_DOWNLOAD,
                DocumentStage.DOWNLOADED,
                DocumentStage.SENT_TO_S3
        );

        List<Document> documentsToProcess = repository.document.findDocumentsInHistoryExcludingStages(excludedStages);

        if (!documentsToProcess.isEmpty()) {
            log.info("Arquivos a serem processados para exclusão: {}", documentsToProcess.size());
            documentsToProcess.parallelStream().forEach(document -> {
                Path filePath = Paths.get(readerDir, readerDownload, document.getFileName());
                try {
                    if (Files.exists(filePath)) {
                        Files.delete(filePath);

                        // Adiciona um novo log de estagio ao documento sem alterar o estagio atual
                        DocumentStageHistory stageLog = DocumentStageHistory.builder()
                                .document(document)
                                .stage(DocumentStage.DELETED_FROM_DOWNLOAD)
                                .lastModified(LocalDateTime.now())
                                .build();
                        repository.documentStageHistory.save(stageLog);
                        repository.document.save(document);

                        log.info("Arquivo {} excluído com sucesso.", document.getFileName());
                    } else {
                        log.warn("Arquivo não encontrado para exclusão: {}", document.getFileName());
                    }
                } catch (IOException e) {
                    log.error("Erro ao limpar arquivo {} da pasta 'baixados': {}", document.getFileName(), e.getMessage());
                }
            });
        } else {
            log.info("Nenhum arquivo encontrado para limpeza.");
        }
    }
}
