package br.com.groupsoftware.grouppay.extratoremail.extractor.core.impl;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentExtractorType;
import br.com.groupsoftware.grouppay.extratoremail.exception.MailReaderException;
import br.com.groupsoftware.grouppay.extratoremail.extractor.core.OcrExtractor;
import br.com.groupsoftware.grouppay.extratoremail.extractor.core.PdfExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Serviço para extração de texto de documentos PDF utilizando PDFBox e OCR.
 * <p>
 * Este serviço tenta primeiro extrair o texto de um PDF usando a biblioteca PDFBox. Caso o conteúdo extraído seja vazio ou inválido,
 * ele utiliza o OCR (Tesseract) para tentar extrair o texto da imagem gerada do PDF.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Slf4j
@Component
@RequiredArgsConstructor
class PdfExtractorImpl implements PdfExtractor {

    @Value("${reader.dir}")
    private String readerDir;

    private final OcrExtractor ocr;
    private static final long TIMEOUT_SECONDS = 60;

    public Document extractText(Document document) throws MailReaderException {
        // Constrói o caminho completo do arquivo incluindo readerDir
        File pdfFile = Paths.get(readerDir, document.getLocalFilePath()).toFile();
        
        if (!pdfFile.exists()) {
            log.error("Arquivo PDF não encontrado: {}", pdfFile.getAbsolutePath());
            throw new MailReaderException("Arquivo PDF não encontrado: " + pdfFile.getAbsolutePath());
        }
        
        log.debug("Extraindo texto do PDF: {}", pdfFile.getAbsolutePath());
        String content = readPdfContentWithPdfBox(pdfFile);

        // Verifica se o conteúdo extraído é válido (não vazio e tem tamanho mínimo)
        // Não exige necessariamente "CNPJ" pois alguns PDFs podem não ter essa palavra
        if (content != null && !content.trim().isEmpty() && content.trim().length() > 10) {
            // Verifica se contém "CNPJ" ou outros indicadores de documento válido
            String upperContent = content.toUpperCase();
            boolean hasValidContent = upperContent.contains("CNPJ") || 
                                     upperContent.contains("CPF") ||
                                     upperContent.contains("CNPJ/CPF") ||
                                     upperContent.matches(".*\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}.*") || // Padrão CNPJ
                                     upperContent.matches(".*\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}.*"); // Padrão CPF
            
            if (hasValidContent) {
                log.info("Texto extraído com sucesso pelo PDFBox (contém indicadores válidos).");
                updateDocumentWithContent(document, content, DocumentExtractorType.PDFBOX);
                return document;
            } else {
                log.warn("Texto extraído pelo PDFBox não contém indicadores válidos (CNPJ/CPF). Tamanho: {} caracteres. Tentando OCR...", content.length());
            }
        } else {
            log.warn("Texto extraído pelo PDFBox é vazio ou muito curto. Tamanho: {}. Tentando OCR...", 
                    content != null ? content.length() : 0);
        }

        log.info("Tentando extrair texto usando OCR com Tesseract...");
        try {
            String email;
            if (Objects.nonNull(document.getClientGroup())) {
                email = document.getClientGroup().getEmail();
            }
            else {
                email = document.getCompany().getEmail();
            }

            Future<String> futureContent = ocr.submitOcrTask(email, document.getFileName());
            content = futureContent.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (content != null && !content.trim().isEmpty()) {
                log.info("Texto extraído com sucesso pelo Tesseract OCR.");
                updateDocumentWithContent(document, content, DocumentExtractorType.OCR);
                return document;
            } else {
                log.info("Tesseract OCR não conseguiu extrair texto significativo.");
            }
        } catch (TimeoutException te) {
            log.error("Timeout ao realizar OCR com Tesseract para o arquivo {}: {}", pdfFile.getAbsolutePath(), te.getMessage(), te);
        } catch (Exception e) {
            log.error("Erro ao realizar OCR com Tesseract para o arquivo {}: {}", pdfFile.getAbsolutePath(), e.getMessage(), e);
        }

        String errorMessage = String.format(
            "Erro ao extrair texto do PDF. Arquivo: %s. PDFBox não conseguiu extrair texto válido e OCR também falhou ou não retornou conteúdo.",
            pdfFile.getAbsolutePath()
        );
        throw new MailReaderException(errorMessage);
    }

    private String readPdfContentWithPdfBox(File pdfFile) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (Exception e) {
            log.error("Erro ao extrair texto com PDFBox: {}", e.getMessage(), e);
            return null;
        }
    }

    private void updateDocumentWithContent(Document document, String content, DocumentExtractorType extractorType) {
        document.setDocumentExtractorTypes(List.of(extractorType));
        // Normalização menos agressiva que preserva caracteres importantes para extração
        // Preserva: letras, números, espaços, pontuação comum, símbolos monetários e especiais
        String extractedTextNormalized = content
                .replaceAll("[^\\p{L}\\p{N}\\s:/.\\-,;()\\[\\]{}|#*+=<>\"'`~^&!?\\\\@$%R]", " ") // remove apenas símbolos realmente indesejados
                .replaceAll("\\s+", " ") // normaliza espaços múltiplos
                .trim();
        String safeExtractedText = extractedTextNormalized.substring(0, Math.min(65500, extractedTextNormalized.length()));
        document.setTextExtracted(safeExtractedText);
    }
}
