package br.com.groupsoftware.grouppay.extratoremail.extractor.core;

import br.com.groupsoftware.grouppay.extratoremail.exception.MailReaderException;

import java.io.File;

/**
 * Interface que define o contrato para extração de conteúdo textual de arquivos PDF,
 * utilizando métodos e processos relacionados a Python, como integração com bibliotecas externas.
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
public interface PythonExtractor {
    String extractText(File pdfFile) throws MailReaderException;
}

