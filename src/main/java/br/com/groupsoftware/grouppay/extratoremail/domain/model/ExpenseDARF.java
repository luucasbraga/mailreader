package br.com.groupsoftware.grouppay.extratoremail.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

/**
 * Representa uma despesa do tipo DARF (Documento de Arrecadação de Receitas Federais) no sistema.
 * <p>
 * Estende a classe {@link Expense} e adiciona informações específicas sobre o DARF, como
 * período de apuração, código de receita, número do documento, código de barras e composição dos tributos.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class ExpenseDARF extends Expense {
    private String razaoSocialContribuinte;
    private String periodoApuracao; // Formato: "Out/2025" ou "10/2025"
    private String numeroDocumento;
    private String numeroRecibo;
    private String codigoBarras; // 48 dígitos
    private String pixCopiaCola;
    private List<TributoDARF> composicao;

    /**
     * Classe interna que representa um tributo individual dentro do DARF.
     */
    @Data
    public static class TributoDARF {
        private String codigoReceita; // Ex: "1082", "1138", "1162"
        private String denominacao; // Nome do tributo
        private BigDecimal principal;
        private BigDecimal multa;
        private BigDecimal juros;
        private BigDecimal total;
    }
}
