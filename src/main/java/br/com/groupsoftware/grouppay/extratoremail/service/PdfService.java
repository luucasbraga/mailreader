package br.com.groupsoftware.grouppay.extratoremail.service;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;
import br.com.groupsoftware.grouppay.extratoremail.exception.MailReaderException;

/**
 * Interface para o serviço de processamento de arquivos PDF.
 * <p>
 * Define os métodos necessários para a análise e extração
 * de dados financeiros de documentos PDF associados a uma entidade {@link Document}.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
public interface PdfService {
    Expense getExpenseFromDocument(Document document) throws MailReaderException;
}
