package br.com.groupsoftware.grouppay.extratoremail.extractor.core;

import java.util.concurrent.Future;

/**
 * Interface que define o contrato para extração de conteúdo textual de arquivos PDF,
 * utilizando métodos e processos relacionados a Python, como integração com bibliotecas externas.
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
public interface OcrExtractor {
    Future<String> submitOcrTask(String username, String pdfName);
}

