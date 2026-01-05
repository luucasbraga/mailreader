package br.com.groupsoftware.grouppay.extratoremail.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

/**
 * Representa uma despesa do tipo FGTS (Guia do FGTS Digital - GFD) no sistema.
 * <p>
 * Estende a classe {@link Expense} e adiciona informações específicas sobre a guia FGTS, como
 * identificador da guia, código de barras/PIX e composição detalhada por competência.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class ExpenseFGTS extends Expense {
    private String razaoSocialEmpregador;
    private String identificador; // Identificador da guia (ex: "0124040202313489-5")
    private String codigoBarras;
    private String pixCopiaCola;
    private List<ComposicaoFGTS> composicao;

    /**
     * Classe interna que representa a composição do FGTS por competência/trabalhador.
     */
    @Data
    public static class ComposicaoFGTS {
        private String competencia; // Ex: "09/2025"
        private Integer quantidadeTrabalhadores;
        private BigDecimal valorRemuneracao;
        private BigDecimal valorFgts;
        private BigDecimal valorTotal;
    }
}
