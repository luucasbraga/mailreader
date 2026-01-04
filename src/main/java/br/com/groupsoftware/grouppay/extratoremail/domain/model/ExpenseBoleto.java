package br.com.groupsoftware.grouppay.extratoremail.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Representa uma despesa do tipo Boleto Bancário no sistema.
 * <p>
 * Estende a classe {@link Expense} e adiciona informações específicas sobre o boleto, como banco emissor, código de barras,
 * linha digitável, cedente, nosso número, juros, multa e descontos aplicáveis.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class ExpenseBoleto extends Expense {
    private String bancoEmissor;
    private String codigoBarras;
    private String linhaDigitavel;
    private String cedente;
    private String nossoNumero;
    private BigDecimal juros;
    private BigDecimal multa;
    private BigDecimal descontos;
}
