package br.com.groupsoftware.grouppay.extratoremail.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Representa uma despesa do tipo Nota Fiscal de Serviços (NFS) no sistema.
 * <p>
 * Estende a classe {@link Expense} e adiciona informações específicas sobre a nota fiscal de serviços,
 * como número da nota, série da nota, código de verificação, descrição do serviço prestado, alíquota e valor do ISS,
 * além de descontos e valor líquido após impostos e descontos.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class ExpenseNFS extends Expense {
    private String codigoVerificacao;
    private String descricaoServico; // Detalhes do serviço prestado
    private BigDecimal aliquotaISS; // Alíquota do ISS (Imposto sobre Serviços)
    private BigDecimal valorISS; // Valor do ISS
    private BigDecimal descontos; // Descontos aplicados
    private BigDecimal valorLiquido; // Valor líquido após impostos e descontos
}