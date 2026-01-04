package br.com.groupsoftware.grouppay.extratoremail.domain.model;

import br.com.groupsoftware.grouppay.extratoremail.domain.enums.ExpenseType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Classe abstrata que representa uma despesa no sistema.
 * <p>
 * Armazena informações gerais sobre a despesa, como dados do emitente, destinatário, datas e valores relacionados ao documento.
 * Esta classe serve como base para tipos específicos de despesas, como notas fiscais, boletos, entre outros.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
//@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public abstract class Expense {
    protected LocalDate dataEmissao; // Data de emissão do documento
    protected LocalDate dataVencimento; // Data de vencimento da despesa, se aplicável
    protected BigDecimal valorTotal; // Valor total da despesa
    protected String emitente; // Nome ou razão social do emitente
    protected String cnpjCpfEmitente; // CNPJ ou CPF do emitente
    protected String numero; //numero do documento
    protected String serie; //serie (somente para notas fiscais)
    protected String cnpjCpfDestinatario; // CNPJ ou CPF do destinatário
    protected ExpenseType expenseType;
    protected String companyUUID;
}