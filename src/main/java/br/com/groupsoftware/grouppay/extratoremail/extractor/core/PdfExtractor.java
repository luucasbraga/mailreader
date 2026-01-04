package br.com.groupsoftware.grouppay.extratoremail.extractor.core;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.exception.MailReaderException;

/**
 * Interface que define o contrato para a extração de conteúdo textual de arquivos PDF.
 * Implementações desta interface devem processar o PDF e retornar os dados extraídos em formato de mapa.
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
public interface PdfExtractor {
    Document extractText(Document document) throws MailReaderException;
}

