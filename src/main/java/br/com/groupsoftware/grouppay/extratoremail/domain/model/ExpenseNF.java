package br.com.groupsoftware.grouppay.extratoremail.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

/**
 * Representa uma despesa do tipo Nota Fiscal (NF) no sistema.
 * <p>
 * Estende a classe {@link Expense} e adiciona informações específicas sobre a nota fiscal, como chave de acesso, número da nota,
 * série da nota, valores de frete, seguro, descontos e itens da nota fiscal, incluindo descrição, quantidade, valor unitário,
 * valor total por item e impostos aplicáveis (ex: ICMS, IPI, PIS).
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class ExpenseNF extends Expense {
    private String chaveAcesso;
    private BigDecimal valorFrete;
    private BigDecimal valorSeguro;
    private BigDecimal descontos;
    private List<ItemNotaFiscal> itens;

    @Data
    public static class ItemNotaFiscal {
        private String descricao;
        private int quantidade;
        private BigDecimal valorUnitario;
        private BigDecimal valorTotalItem;
        private List<Imposto> impostos;
    }

    @Data
    public static class Imposto {
        private String tipoImposto; // Ex: ICMS, IPI, PIS
        private BigDecimal valor;
    }
}
