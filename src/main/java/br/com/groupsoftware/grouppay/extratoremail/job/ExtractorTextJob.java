package br.com.groupsoftware.grouppay.extratoremail.job;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentStage;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.ExpenseType;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.Status;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;
import br.com.groupsoftware.grouppay.extratoremail.extractor.core.ExtractorFacade;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import br.com.groupsoftware.grouppay.extratoremail.service.ServiceFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

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
public class ExtractorTextJob implements Job {

    @Value("${document-processing-retry-delay}")
    private int documentProcessingRetryDelay;

    private final RepositoryFacade repository;
    private final ExtractorFacade extractor;
    private final ServiceFacade service;

    @Override
    public void execute(JobExecutionContext context) {
        log.info("[TEXT_EXTRACTION] Iniciando o job de extração de dados dos documentos.");

        // Busca documentos que estejam com senha removida e ainda não foram processados
        LocalDateTime minDateTime = LocalDateTime.now().minusMinutes(documentProcessingRetryDelay);
        List<Document> documentsToProcess = repository.document.findDocumentsEligibleForProcessing(DocumentStage.PASSWORD_REMOVED, minDateTime);
        if (!documentsToProcess.isEmpty()) {
            log.info("Extraindo texto de {} documentos.", documentsToProcess.size());
            documentsToProcess.parallelStream().forEach(document -> {
                try {
                    service.document.changeStatus(document, Status.PROCESSING);
                    Document mDocument = extractor.pdf.extractText(document);
                    if (mDocument != null) {
                        if (!service.document.existsTextExtracted(document)) {
                            service.document.changeStage(mDocument, DocumentStage.TEXT_EXTRACTED);
                        } else {
                            processExistingDocument(document, mDocument.getExpenseType());
                        }
                    }
                } catch (Exception e) {
                    log.error("Erro ao extrair dados do documento: {}", document.getFileName(), e);
                    service.document.changeStage(document, DocumentStage.ERRO);
                } finally {
                    deletePdfRelatedImages(document.getLocalFilePath());
                    service.document.changeStatus(document, Status.NOT_PROCESSING);
                }
            });
        } else {
            log.info("Nenhum PDF encontrado para extração de dados.");
        }
    }

    protected void processExistingDocument(Document document, ExpenseType type) {
        log.warn("Tipo de documento desconhecido: {}", type);
        document.setStage(DocumentStage.DELETE_FROM_LOCAL);
        document.addStageHistoryEntry();
        repository.document.save(document);
    }

    public static void deletePdfRelatedImages(String pdfFilePath) {
        try {
            // Suponha que o nome da imagem seja o mesmo nome do PDF seguido por um número e .png
            Path pdfPath = Paths.get(pdfFilePath);
            String fileNameWithoutExtension = pdfPath.getFileName().toString().replaceAll("\\.pdf$", "");

            // Define o padrão para encontrar as imagens relacionadas ao PDF (nomePDF-número.png)
            Pattern pattern = Pattern.compile("^" + Pattern.quote(fileNameWithoutExtension) + "-\\d+\\.png$");

            // Caminho do diretório onde as imagens podem estar
            File directory = pdfPath.getParent().toFile();

            // Lista todos os arquivos no diretório
            File[] files = directory.listFiles((dir, name) -> pattern.matcher(name).matches());

            if (files != null) {
                for (File file : files) {
                    if (file.exists()) {
                        boolean deleted = file.delete();
                        if (deleted) {
                            System.out.println("Imagem deletada com sucesso: " + file.getName());
                        } else {
                            System.err.println("Erro ao deletar a imagem: " + file.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao tentar excluir as imagens: " + e.getMessage());
        }
    }
}