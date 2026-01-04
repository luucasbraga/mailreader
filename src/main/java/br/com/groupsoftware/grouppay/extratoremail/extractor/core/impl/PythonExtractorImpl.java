package br.com.groupsoftware.grouppay.extratoremail.extractor.core.impl;

import br.com.groupsoftware.grouppay.extratoremail.exception.MailReaderException;
import br.com.groupsoftware.grouppay.extratoremail.extractor.core.PythonExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Serviço para extração de texto de PDFs utilizando um script Python externo.
 *
 * <p>Este serviço executa um script Python para realizar a extração de texto de um arquivo PDF.
 * O script é chamado através de um processo do sistema operacional, e o texto extraído é capturado
 * a partir da saída do script.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */

@Slf4j
@Component
class PythonExtractorImpl implements PythonExtractor {

    @Value("${python.scripts}")
    private String scriptPath;

    @Value("${python.path}")
    private String pythonPath;

    public String extractText(File pdfFile) throws MailReaderException {
        try {
            // Resolva o caminho relativo para o script
            String absoluteScriptPath = new File(scriptPath).getCanonicalPath();
            log.info("Executando script Python no caminho: {}", absoluteScriptPath);

            // Monta o comando para executar o Python
            String[] command = {pythonPath, absoluteScriptPath, pdfFile.getAbsolutePath()};
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new MailReaderException("Erro ao executar script Python. Código de saída: " + exitCode);
                }

                return output.toString().trim();
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MailReaderException("Erro ao processar o PDF: " + e.getMessage());
        }
    }
}
