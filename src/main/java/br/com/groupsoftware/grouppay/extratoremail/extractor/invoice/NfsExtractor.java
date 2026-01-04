package br.com.groupsoftware.grouppay.extratoremail.extractor.invoice;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;
import br.com.groupsoftware.grouppay.extratoremail.extractor.ExpenseExtractor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ‘Interface’ que define o contrato para extração de despesas a partir de textos de documentos NFS.
 * Implementações desta ‘interface’ devem transformar o conteúdo textual numa instância de {@link Expense}.
 *
 * <p>Essa ‘interface’ é projetada para processar dados financeiros de documentos de Notas Fiscais de Serviço (NFS-e),
 * convertendo o texto extraído num objeto estruturado de despesa, levando em consideração o tipo de documento.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
public interface NfsExtractor extends ExpenseExtractor {
    Expense getExpense(Document document);
}
