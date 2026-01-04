package br.com.groupsoftware.grouppay.extratoremail.extractor.slip;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;
import br.com.groupsoftware.grouppay.extratoremail.extractor.ExpenseExtractor;

/**
 * ‘Interface’ que define o contrato para extração de despesas a partir de textos de faturas.
 * <p>
 * Implementações desta ‘interface’ devem transformar o conteúdo textual extraído do documento
 * numa instância de {@link Expense}, garantindo que os dados financeiros sejam convertidos
 * de forma estruturada e padronizada.
 * </p>
 * <p>
 * Essa ‘interface’ é utilizada para processar e extrair informações essenciais de faturas,
 * tais como valores, datas de vencimento e dados dos envolvidos, permitindo o seu uso em sistemas
 * de controle financeiro e análise de despesas.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2025
 */
public interface BillExtractor extends ExpenseExtractor {
    Expense getExpense(Document document);
}