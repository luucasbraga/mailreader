package br.com.groupsoftware.grouppay.extratoremail.extractor.core;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.ExpenseType;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;

/**
 * Interface que define o contrato para extração de despesas a partir de textos de documentos NF3.
 * Implementações desta interface devem transformar o conteúdo textual em uma instância de {@link Expense}.
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
public interface GeminiExtractor {
    Expense getExpense(Document document, ExpenseType type);
}
