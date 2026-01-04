package br.com.groupsoftware.grouppay.extratoremail.extractor.core.impl;

import br.com.groupsoftware.grouppay.extratoremail.extractor.core.OcrExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Implementação do extrator de OCR (Reconhecimento Ótico de Caracteres) para processamento de documentos PDF.
 * <p>
 * Esta classe utiliza o Tesseract para realizar o OCR sobre as imagens extraídas de documentos PDF.
 * O processamento é feito num contêiner Docker quando o ambiente de desenvolvimento está em uso,
 * e diretamente no sistema operativo em produção.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OcrExtractorImpl implements OcrExtractor {

    private static final String DOCKER_CONTAINER_NAME = "tesseract-container";
    private final Environment environment;
    // Usando um pool de threads para processamento concorrente de OCR
    private final ExecutorService ocrExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Override
    public Future<String> submitOcrTask(String username, String pdfName) {
        // Delega a execução para o pool de threads
        return ocrExecutor.submit(() -> executeOcrTaskInternal(username, pdfName));
    }

    private String executeOcrTaskInternal(String username, String pdfName) {
        String baseOriginalFilePrefix = pdfName.replace(".pdf", ""); // Ex: "meu_documento"
        String userPath = environment.acceptsProfiles(Profiles.of("dev"))
                ? String.format("/pdfs/%s", username) // Caminho para ambiente de desenvolvimento
                : String.format("/usr/local/tomcat/webapps/PDF/%s", username); // Caminho para produção
        String pdfFilePath = String.format("%s/%s", userPath, pdfName);

        // 1. Gerar um ID único para esta tarefa de OCR específica
        String uniqueTaskId = UUID.randomUUID().toString();
        // Criar um prefixo de arquivo único para as imagens desta tarefa, dentro do userPath
        // Ex: /usr/local/tomcat/webapps/PDF/username/uuid_meu_documento
        String uniqueImageFileBasePrefix = uniqueTaskId + "_" + baseOriginalFilePrefix;
        String fullImageOutputPrefix = Paths.get(userPath, uniqueImageFileBasePrefix).toString();

        String imageExtension = ".tif"; // MUDANÇA: Usar TIFF como formato intermediário
        List<String> generatedImageFilePaths = new ArrayList<>(); // Lista para rastrear arquivos criados para limpeza

        try {
            // 2. Preparar e executar o comando pdftoppm para converter PDF para imagens TIFF
            ProcessBuilder extractImagesProcessBuilder;
            if (environment.acceptsProfiles(Profiles.of("dev"))) {
                extractImagesProcessBuilder = new ProcessBuilder("docker", "exec", DOCKER_CONTAINER_NAME,
                        "pdftoppm", "-tiff", "-r", "300", pdfFilePath, fullImageOutputPrefix);
            } else {
                extractImagesProcessBuilder = new ProcessBuilder("pdftoppm", "-tiff", "-r", "300", pdfFilePath, fullImageOutputPrefix);
            }

            extractImagesProcessBuilder.environment().put("OMP_THREAD_LIMIT", "1");
            extractImagesProcessBuilder.redirectErrorStream(true); // Captura stdout e stderr juntos

            log.debug("[OCR Task {}] Executando pdftoppm: {}", uniqueTaskId, String.join(" ", extractImagesProcessBuilder.command()));
            Process extractProcess = extractImagesProcessBuilder.start();

            // Capturar a saída do pdftoppm para depuração
            StringBuilder pdftoppmOutput = new StringBuilder();
            try (BufferedReader processReader = new BufferedReader(new InputStreamReader(extractProcess.getInputStream()))) {
                String line;
                while ((line = processReader.readLine()) != null) {
                    pdftoppmOutput.append(line).append(System.lineSeparator());
                }
            }
            int extractExitCode = extractProcess.waitFor();

            if (extractExitCode != 0) {
                log.error("[OCR Task {}] pdftoppm falhou para o PDF: {}. Código de saída: {}. Saída: {}",
                        uniqueTaskId, pdfFilePath, extractExitCode, pdftoppmOutput.toString().trim());
                // Retorna a saída do pdftoppm para que possa ser incluída na exceção ou log superior
                return "Error extracting images from PDF: " + pdfFilePath + ". pdftoppm output: " + pdftoppmOutput.toString().trim();
            }
            log.info("[OCR Task {}] pdftoppm concluído para o PDF: {}. Imagens geradas com prefixo: {}",
                    uniqueTaskId, pdfFilePath, fullImageOutputPrefix);

            // 3. Processar as imagens TIFF geradas com o Tesseract
            List<String> ocrPageResults = new ArrayList<>();
            for (int i = 1; ; i++) {
                // Constrói o caminho para o arquivo de imagem específico da página e da tarefa
                String currentImageFilePath = String.format("%s-%d%s", fullImageOutputPrefix, i, imageExtension);
                File imageFile = new File(currentImageFilePath);

                if (!imageFile.exists()) { // Verifica se a imagem da próxima página existe
                    if (i == 1) { // Nenhuma imagem foi gerada
                        log.warn("[OCR Task {}] Nenhuma imagem (ex: {}-1{}) encontrada após pdftoppm para o PDF: {}",
                                uniqueTaskId, uniqueImageFileBasePrefix, imageExtension, pdfFilePath);
                    }
                    break; // Sai do loop se não houver mais imagens
                }
                generatedImageFilePaths.add(currentImageFilePath); // Adiciona à lista para limpeza posterior

                ProcessBuilder ocrProcessBuilder;
                if (environment.acceptsProfiles(Profiles.of("dev"))) {
                    ocrProcessBuilder = new ProcessBuilder("docker", "exec", DOCKER_CONTAINER_NAME,
                            "tesseract", currentImageFilePath, "stdout", "-l", "por+eng");
                } else {
                    ocrProcessBuilder = new ProcessBuilder("tesseract", currentImageFilePath, "stdout", "-l", "por+eng");
                }

                ocrProcessBuilder.environment().put("OMP_THREAD_LIMIT", "1");
                ocrProcessBuilder.redirectErrorStream(true);

                log.debug("[OCR Task {}] Executando Tesseract: {}", uniqueTaskId, String.join(" ", ocrProcessBuilder.command()));
                Process ocrProcess = ocrProcessBuilder.start();

                StringBuilder tesseractOutput = new StringBuilder();
                try (BufferedReader processReader = new BufferedReader(new InputStreamReader(ocrProcess.getInputStream()))) {
                    String line;
                    while ((line = processReader.readLine()) != null) {
                        tesseractOutput.append(line).append(System.lineSeparator());
                    }
                }
                int ocrExitCode = ocrProcess.waitFor();
                String tesseractOutputString = tesseractOutput.toString().trim();

                if (ocrExitCode != 0) {
                    log.warn("[OCR Task {}] Tesseract falhou para a imagem {}. Código de saída: {}. Saída: {}",
                            uniqueTaskId, currentImageFilePath, ocrExitCode, tesseractOutputString);
                    // Considerar se deve continuar para as próximas imagens ou interromper.
                    // A lógica atual interrompe, o que geralmente é apropriado se uma página falhar criticamente.
                    break;
                }

                if (tesseractOutputString.isEmpty()) {
                    log.info("[OCR Task {}] Tesseract processou a imagem {} com sucesso, mas não extraiu texto.", uniqueTaskId, currentImageFilePath);
                } else {
                    log.info("[OCR Task {}] Tesseract extraiu texto com sucesso da imagem {}.", uniqueTaskId, currentImageFilePath);
                }
                ocrPageResults.add(tesseractOutputString);
            }

            if (ocrPageResults.isEmpty() && generatedImageFilePaths.isEmpty()) {
                log.warn("[OCR Task {}] Nenhuma imagem foi gerada ou processada para o PDF: {}", uniqueTaskId, pdfName);
            } else if (ocrPageResults.isEmpty()) {
                log.warn("[OCR Task {}] Processo OCR concluído, mas nenhum resultado de texto foi coletado para o PDF: {}", uniqueTaskId, pdfName);
            }

            // Junta os resultados de cada página, filtrando strings vazias que podem ter sido adicionadas.
            return ocrPageResults.stream().filter(s -> s != null && !s.isEmpty()).collect(Collectors.joining("\n\n"));

        } catch (IOException | InterruptedException e) {
            log.error("[OCR Task {}] Exceção durante a execução do OCR para o PDF {}: {}", uniqueTaskId, pdfName, e.getMessage(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt(); // Preserva o status de interrupção da thread
            }
            return "Error during OCR execution: " + e.getMessage();
        } finally {
            // 4. Limpar os arquivos de imagem TIFF gerados especificamente para esta tarefa
            if (!generatedImageFilePaths.isEmpty()) {
                log.info("[OCR Task {}] Limpando {} arquivos de imagem gerados para o prefixo base '{}' no caminho '{}'",
                        uniqueTaskId, generatedImageFilePaths.size(), uniqueImageFileBasePrefix, userPath);
                for (String imagePathToDelete : generatedImageFilePaths) {
                    try {
                        Files.deleteIfExists(Paths.get(imagePathToDelete));
                        // log.debug("[OCR Task {}] Arquivo de imagem deletado: {}", uniqueTaskId, imagePathToDelete); // Log opcional por arquivo
                    } catch (IOException ex) {
                        log.error("[OCR Task {}] Falha ao deletar arquivo de imagem: {}. Erro: {}", uniqueTaskId, imagePathToDelete, ex.getMessage());
                    }
                }
            }
        }
    }
}
