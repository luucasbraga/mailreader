package br.com.groupsoftware.grouppay.extratoremail.extractor.invoice;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;
import br.com.groupsoftware.grouppay.extratoremail.extractor.ExpenseExtractor;

/**
 * ‘Interface’ que define o contrato para extração de despesas a partir de textos de documentos NF.
 * Implementações desta ‘interface’ devem transformar o conteúdo textual numa instância de {@link Expense}.
 *
 * <p>Essa ‘interface’ é projetada para processar e extrair dados financeiros de documentos de notas fiscais,
 * convertendo o texto extraído num objeto estruturado de despesa, com base no tipo de documento especificado.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
public interface NfExtractor extends ExpenseExtractor {
    Expense getExpense(Document document);
}
