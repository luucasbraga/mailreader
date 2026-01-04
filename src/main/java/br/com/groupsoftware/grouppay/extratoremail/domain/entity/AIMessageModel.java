package br.com.groupsoftware.grouppay.extratoremail.domain.entity;

import br.com.groupsoftware.grouppay.extratoremail.domain.enums.AiPlanType;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.ExpenseType;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

/**
 * Entidade que representa os modelos de mensagens JSON de retorno para diferentes tipos de documentos.
 * <p>
 * Esta classe mapeia a tabela tb_ai_message_model no banco de dados e contém os campos que representam o tipo de documento
 * (ExpenseType), o modelo JSON associado a esse documento e o tipo de plano (AiPlanType) relacionado.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Entity
@Table(name = "tb_ai_message_model")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIMessageModel implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "DOCUMENT_TYPE", nullable = false)
    private ExpenseType expenseType;

    @Lob
    @Column(name = "DOCUMENT_RULES", columnDefinition = "MEDIUMTEXT")
    private String rules;

    @Lob
    @Column(name = "JSON_MODEL", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String jsonModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "PLAN_TYPE", nullable = false)
    private AiPlanType planType; // "COMPLETO" ou "BASICO"

    @Override
    public String toString() {
        return "AIMessageModel{" +
                "id=" + id +
                ", expenseType=" + expenseType +
                ", rules='" + rules + '\'' +
                ", jsonModel='" + jsonModel + '\'' +
                ", planType=" + planType +
                '}';
    }
}
