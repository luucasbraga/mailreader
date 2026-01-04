package br.com.groupsoftware.grouppay.extratoremail.extractor.slip;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;
import br.com.groupsoftware.grouppay.extratoremail.extractor.ExpenseExtractor;

/**
 * Interface que define o contrato para extração de despesas a partir de guias FGTS
 * (Guia do FGTS Digital - GFD).
 * <p>
 * Implementações desta interface devem transformar o conteúdo textual extraído do documento
 * numa instância de {@link Expense}, garantindo que os dados trabalhistas sejam convertidos
 * de forma estruturada e padronizada.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2025
 */
public interface FgtsExtractor extends ExpenseExtractor {
    Expense getExpense(Document document);
}
