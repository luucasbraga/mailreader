package br.com.groupsoftware.grouppay.extratoremail.domain.entity;

import br.com.groupsoftware.grouppay.extratoremail.domain.enums.ExpenseType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;

/**
 * Representa os padrões regex utilizados para extrair informações de Notas Fiscais (NF).
 * <p>
 * Esta classe armazena regex para extrair dados como nome do emissor, CNPJ, data de emissão, valor total,
 * chave de acesso, e outros dados específicos dependendo do tipo de nota fiscal (NF comum ou NFS-e).
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Entity
@Table(name = "tb_regex", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"TYPE", "IBGE_CODE"})
})
@Getter
@Setter

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Regex implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @NotNull
    @Column(name = "ISSUER_CNPJ")
    private String issuerCNPJ;  // Regex para CNPJ do emissor

    @NotNull
    @Column(name = "TOTAL_VALUE")
    private String totalValue;  // Regex para valor total

    @NotNull
    @Column(name = "DUE_DATE")
    private String dueDate;  // Regex para data de vencimento

    @NotNull
    @Column(name = "ISSUE_DATE")
    private String issueDate;  // Regex para data de emissão/referencia

    @Column(name = "NUMBER")
    private String number;  // Regex para número do documento

    @Column(name = "ISSUER_NAME")
    private String issuerName; //Regex para nome do emissor

    @Column(name = "SERIE")
    private String serie;  // Regex para série

    @Column(name = "IBGE_CODE", nullable = false)
    private String ibgeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", nullable = false)
    private ExpenseType expenseType;

    @Override
    public String toString() {
        return "RegexTemplate{" +
                "id=" + id +
                ", regexIssuerCNPJ='" + issuerCNPJ + '\'' +
                ", issuerName='" + issuerName + '\'' +
                ", totalValue='" + totalValue + '\'' +
                ", dueDate='" + dueDate + '\'' +
                ", issueDate='" + issueDate + '\'' +
                ", number='" + number + '\'' +
                ", serieNf='" + serie + '\'' +
                ", ibgeCode='" + ibgeCode + '\'' +
                ", expenseType=" + expenseType +
                '}';
    }
}