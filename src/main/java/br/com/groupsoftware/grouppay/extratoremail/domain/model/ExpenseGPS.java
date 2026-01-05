package br.com.groupsoftware.grouppay.extratoremail.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Representa uma despesa do tipo GPS (Guia da Previdência Social) no sistema.
 * <p>
 * Estende a classe {@link Expense} e adiciona informações específicas sobre a GPS, como
 * código de pagamento, competência, identificador e valores de INSS.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class ExpenseGPS extends Expense {
    private String razaoSocialContribuinte;
    private String codigoPagamento; // Ex: "2100", "2208"
    private String competencia; // Ex: "10/2025"
    private String identificador; // CEI ou NIT
    private String codigoBarras;
    private String pixCopiaCola;
    private BigDecimal valorINSS;
    private BigDecimal valorOutrasEntidades;
    private BigDecimal atualizacaoMonetaria;
    private BigDecimal juros;
    private BigDecimal multa;
}
