package br.com.groupsoftware.grouppay.extratoremail.service;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.DocumentDTO;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.ExpenseDTO;

/**
 * Serviço responsável pelo envio de despesas para sistemas externos.
 *
 * <p>Define a operação necessária para processar e enviar objetos do tipo {@link ExpenseDTO}
 * associados a um {@link Document} para um sistema externo ou endpoint.</p>
 *
 * <p>Implementações desta interface devem garantir a comunicação adequada com o sistema
 * de destino e tratar possíveis erros relacionados ao envio.</p>
 *
 * @author Marco
 * @version 1.0
 * @since 2024
 */
public interface ExpenseSenderService {
    void sendExpense(Document document, ExpenseDTO expenseDTO);
    DocumentDTO getResultExpense(Document document);
}
