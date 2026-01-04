package br.com.groupsoftware.grouppay.extratoremail.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

/**
 * Representa uma despesa do tipo Fatura no sistema.
 * <p>
 * Estende a classe {@link Expense} e adiciona informações sobre o valor pago e os itens da fatura,
 * incluindo descrição, quantidade, valor unitário e valor total por item.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class ExpenseFatura extends Expense {
    private List<ItemFatura> itens;

    @Data
    public static class ItemFatura {
        private String descricao;
        private int quantidade;
        private BigDecimal valorUnitario;
        private BigDecimal valorTotalItem;
    }
}
