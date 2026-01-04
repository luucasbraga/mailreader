package br.com.groupsoftware.grouppay.extratoremail.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Representa uma despesa do tipo Nota Fiscal de Consumidor (NFC) no sistema.
 * <p>
 * Estende a classe {@link ExpenseNF} e pode omitir alguns detalhes ou incluir campos específicos de consumidor,
 * dependendo das necessidades de processamento.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class ExpenseNFC extends ExpenseNF {
    // Pode omitir alguns detalhes ou incluir campos específicos de consumidor
}
