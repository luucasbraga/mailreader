package br.com.groupsoftware.grouppay.extratoremail.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

/**
 * Representa uma despesa do tipo Nota Fiscal de Energia Elétrica (NF3) no sistema.
 * <p>
 * Estende a classe {@link ExpenseNF} e adiciona informações específicas sobre a nota fiscal de energia elétrica,
 * como o número do medidor e o período de consumo (início e fim).
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class ExpenseNF3 extends ExpenseNF {
    private String numeroMedidor;
    private LocalDate periodoInicio;
    private LocalDate periodoFim;
}