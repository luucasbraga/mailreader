package br.com.groupsoftware.grouppay.extratoremail.service;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentStage;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.Status;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;
import br.com.groupsoftware.grouppay.extratoremail.exception.MailReaderException;

import java.nio.file.Path;

/**
 * Interface para o serviço de processamento de documentos.
 * <p>
 * Define o método necessário para processar arquivos PDF e extrair
 * informações financeiras, retornando objetos {@link Expense}.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
public interface DocumentService {
    Expense processPdfFile(Path pdf, String fileNameOrSuffix) throws MailReaderException;
    void changeStatus(Document document, Status newStatus);
    void changeStage(Document document, DocumentStage newStage);
    boolean existsTextExtracted(Document document);
}