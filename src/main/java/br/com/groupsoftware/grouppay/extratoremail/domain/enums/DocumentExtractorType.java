package br.com.groupsoftware.grouppay.extratoremail.domain.enums;

/**
 * Enumeração que representa as possíveis origens de integração no sistema.
 * <p>
 * Define os diferentes tipos de extratores de documentos utilizados no sistema,
 * como PDFBox, OCR, OpenAI e Python.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
public enum DocumentExtractorType {
    PDFBOX, OCR, OPENAI, PYTHON, TIKA
}
