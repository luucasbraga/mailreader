package br.com.groupsoftware.grouppay.extratoremail.domain.entity;

import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentStage;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entidade que representa um log de estagios de um documento,
 * utilizado para rastrear as mudanças no estagio ao longo do tempo.
 * Cada log de estagio é gerado quando o estagio de um documento é alterado.
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */

@Entity
@Table(name = "tb_document_stage_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentStageHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "DOCUMENT_ID", nullable = false)
    private Document document;

    @Enumerated(EnumType.STRING)
    @Column(name = "STAGE", nullable = false)
    private DocumentStage stage;

    @Column(name = "LAST_MODIFIED", nullable = false)
    private LocalDateTime lastModified;

    @Override
    public String toString() {
        return "DocumentStageHistory{" +
                "id=" + id +
                ", document=" + document +
                ", stage=" + stage +
                ", lastModified=" + lastModified +
                '}';
    }
}