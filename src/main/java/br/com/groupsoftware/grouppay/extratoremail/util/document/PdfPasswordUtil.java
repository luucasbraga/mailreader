package br.com.groupsoftware.grouppay.extratoremail.util.document;

import com.itextpdf.text.pdf.PdfReader;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Utilitário para manipulação de senhas em arquivos PDF.
 * Esta classe fornece métodos para a descriptografia de arquivos PDF protegidos por senha e uso do Tesseract OCR.
 * <p>Este utilitário permite quebrar a senha de um PDF protegido, remover a senha do documento e salvar a versão sem proteção.</p>
 * <p>Os métodos utilizam ferramentas como pdfcrack para quebrar senhas e PDFBox para manipulação dos documentos.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Slf4j
@UtilityClass
public class PdfPasswordUtil {

    public boolean crackAndRemovePassword(String pdfFilePath, String outputFilePath, Environment environment) {
        File pdfFile = new File(pdfFilePath);
        String pdfFileName = pdfFile.getName();

        if (!pdfFile.exists()) {
            log.error("Arquivo não encontrado: {}", pdfFile.getAbsolutePath());
            return false;
        }

        String[] command = environment.acceptsProfiles(Profiles.of("dev")) ?
                new String[]{"docker", "exec", "pdfcrack-container", "pdfcrack", "--file=/pdfs/" + pdfFileName,
                        "--charset=0123456789", "--minpw=1", "--maxpw=6"} :
                new String[]{"pdfcrack", "--file=" + pdfFilePath, "--charset=0123456789", "--minpw=1", "--maxpw=6"};

        String password = tryPdfPasswordWithDocker(command);

        if (password == null) {
            log.error("Falha ao quebrar a senha do PDF {}", pdfFilePath);
            return false;
        }
        return removePasswordFromPdf(pdfFilePath, outputFilePath, password);
    }

    public boolean removePasswordFromPdf(String pdfFilePath, String outputFilePath, String userPassword) {
        try (PDDocument document = PDDocument.load(new File(pdfFilePath), userPassword)) {
            document.setAllSecurityToBeRemoved(true);
            document.save(outputFilePath);
            log.info("PDF descriptografado e salvo sem senha em: {}", outputFilePath);
            return true;
        } catch (IOException e) {
            log.error("Erro ao descriptografar e salvar o PDF: {}", e.getMessage(), e);
            return false;
        }
    }

    public String tryPdfPasswordWithDocker(String[] command) {
        String password = "";
        try {
            // Executa o comando
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);  // Combina saída e erro
            Process process = processBuilder.start();

            // Lê a saída do processo
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    //log.info("Saída: {}", line);
                    if (line.contains("found user-password")) {
                        password = line.split(":")[1].trim().replace("'", "");
                        log.info("Senha encontrada: {}", password);
                        return password;
                    }
                }
            }

            // Verifica o código de saída do processo
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("Processo do pdfcrack concluído com sucesso.");
            } else {
                log.info("Falha ao encontrar a senha.");
            }
        } catch (IOException | InterruptedException e) {
            log.error("Erro ao obter senha do pdf: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
        return password;
    }

    public boolean isPdfEncrypted(String pdfFilePath) {
        PdfReader reader = null;
        try {
            // Tenta abrir o PDF sem senha
            reader = new PdfReader(pdfFilePath);
            // Verifica se o PDF é criptografado
            return reader.isEncrypted();
        } catch (IOException e) {
            log.error("Erro ao ler o PDF: {}", e.getMessage());
            return true;  // Considera que o PDF é criptografado em caso de erro
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
}
