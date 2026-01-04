package br.com.groupsoftware.grouppay.extratoremail.domain.entity;

import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentExtractorType;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentStage;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.ExpenseType;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.Status;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade que representa um documento no sistema, contendo informações sobre o arquivo,
 * o estágio do processamento, o tipo de documento e o histórico de estágio.
 * <p>
 * Com a nova arquitetura, o documento inicialmente está associado apenas ao ClientGroup
 * que recebeu o email. Após o processo de matching, é associado à Company específica.
 * </p>
 *
 * @author Marco Willy
 * @version 2.0 - Atualizada para arquitetura de matching por ClientGroup
 * @since 2024
 */

@Entity
@Table(name = "tb_document")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    // Relacionamento com ClientGroup (obrigatório - define quem recebeu o email)
    @ManyToOne
    @JoinColumn(name = "CLIENT_GROUP_ID", nullable = false)
    @JsonIgnore
    private ClientGroup clientGroup;

    // Relacionamento com Company (opcional - definido após matching)
    @ManyToOne
    @JoinColumn(name = "COMPANY_ID")
    @JsonIgnore
    private Company company;

    @Column(name = "FILE_NAME")
    private String fileName;

    @Column(name = "AMAZON_PATH")
    private String amazonPath;

    @Column(name = "MESSAGE_ID")
    private String messageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "STAGE")
    private DocumentStage stage;

    @Column(name = "EXPENSE_JSON", columnDefinition = "LONGTEXT")
    private String expenseJson;

    @Column(name = "TEXT_EXTRACTED", columnDefinition = "LONGTEXT")
    private String textExtracted;

    @Enumerated(EnumType.STRING)
    @Column(name = "EXPENSE_TYPE")
    private ExpenseType expenseType;

    @ElementCollection(targetClass = DocumentExtractorType.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "DOCUMENT_EXTRACTOR_TYPES", joinColumns = @JoinColumn(name = "DOCUMENT_ID"))
    @Enumerated(EnumType.STRING)
    @Column(name = "DOCUMENT_EXTRACTOR")
    private List<DocumentExtractorType> documentExtractorTypes = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonIgnore
    private List<DocumentStageHistory> stagesHistory = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "DOCUMENT_STATUS")
    private Status status;

    @PrePersist
    @PreUpdate
    public void addStageHistoryEntry() {
        if (this.status == null) {
            this.status = Status.NOT_PROCESSING;
        }
        if (stagesHistory == null) {
            stagesHistory = new ArrayList<>();
        }
        DocumentStageHistory historyEntry = DocumentStageHistory.builder()
                .document(this)
                .stage(this.stage)
                .lastModified(LocalDateTime.now())
                .build();
        stagesHistory.add(historyEntry);
    }

    /**
     * Retorna o caminho local do arquivo.
     * Se a Company já foi identificada, usa o diretório da ClientGroup.
     * Caso contrário, usa um diretório temporário.
     */
    public String getLocalFilePath() {
        if (clientGroup != null && clientGroup.getEmail() != null) {
            return Paths.get(clientGroup.getEmail(), fileName).toString();
        }
        return Paths.get("temp", fileName).toString();
    }

    @Override
    public String toString() {
        return "Document{" +
                "id=" + id +
                ", clientGroup=" + (clientGroup != null ? clientGroup.getUuid() : "null") +
                ", company=" + (company != null ? company.getUuid() : "null") +
                ", fileName='" + fileName + '\'' +
                ", amazonPath='" + amazonPath + '\'' +
                ", messageId='" + messageId + '\'' +
                ", stage=" + stage +
                ", expenseType=" + expenseType +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}