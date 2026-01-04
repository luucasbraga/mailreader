package br.com.groupsoftware.grouppay.extratoremail.service;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;

/**
 * Interface para o serviço de processamento e descriptografia de arquivos PDF.
 * <p>
 * Define os métodos necessários para a descriptografia e processamento de
 * arquivos PDF no sistema.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
public interface DecryptPdfService {
    void decryptPdf(Document document);
}
