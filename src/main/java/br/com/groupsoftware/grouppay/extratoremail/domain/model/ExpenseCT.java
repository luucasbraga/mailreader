package br.com.groupsoftware.grouppay.extratoremail.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Representa uma despesa do tipo Conhecimento de Transporte (CT) no sistema.
 * <p>
 * Estende a classe {@link Expense} e adiciona informações específicas sobre o transporte, como remetente, destinatário,
 * veículo, peso da carga, tipo de carga e motorista responsável.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class ExpenseCT extends Expense {
    private String remetente;
    private String veiculo;
    private BigDecimal pesoCarga;
    private String tipoCarga;
    private String motorista;
}