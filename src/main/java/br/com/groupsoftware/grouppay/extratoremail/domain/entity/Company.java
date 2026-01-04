package br.com.groupsoftware.grouppay.extratoremail.domain.entity;

import br.com.groupsoftware.grouppay.extratoremail.domain.enums.Status;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidade que representa uma empresa no sistema.
 * <p>
 * Armazena informações relacionadas a uma empresa, como CNPJ, nome fantasia,
 * razão social e associações com grupo de clientes e município.
 * Com a nova arquitetura, a empresa não possui mais email próprio,
 * sendo os emails centralizados no ClientGroup.
 * </p>
 *
 * @author Marco Willy
 * @version 2.0 - Atualizada para arquitetura centralizada de emails
 * @since 2024
 */

@Entity
@Table(name = "tb_company")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Deprecated
    @Column(name = "EMAIL", unique = true, nullable = false)
    private String email;

    @Column(name = "UUID", unique = true, nullable = false)
    private String uuid;

    @ManyToOne
    @JoinColumn(name = "CITY_IBGE_CODE", nullable = false)
    private City city;

    @ManyToOne
    @JoinColumn(name = "CLIENT_GROUP_ID")
    private ClientGroup clientGroup;

    @Column(name = "ACTIVE")
    private boolean active;

    @Column(name = "CNPJ", unique = true)
    private String cnpj;

    @Deprecated
    @Column(name = "LAST_MAIL_READ")
    private LocalDateTime lastMailRead;

    // Novos campos para identificação do condomínio
    @Column(name = "FANTASY_NAME")
    private String fantasyName;

    @Deprecated
    @Column(name = "LOCAL_PATH")
    private String localPath;

    @Deprecated
    @Enumerated(EnumType.STRING)
    @Column(name = "EMAIL_STATUS")
    private Status status;

    @Column(name = "LEGAL_NAME")
    private String legalName;

    @OneToMany(mappedBy = "company")
    @JsonIgnore
    private List<Document> pdfs;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "company", cascade = CascadeType.ALL)
    private EmailSearchConfig emailSearchConfig;

    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = Status.NOT_PROCESSING;
        }
    }

    @Override
    public String toString() {
        return "Company{" +
                "id=" + id +
                ", uuid='" + uuid + '\'' +
                ", cnpj='" + cnpj + '\'' +
                ", fantasyName='" + fantasyName + '\'' +
                ", legalName='" + legalName + '\'' +
                ", active=" + active +
                ", clientGroup=" + (clientGroup != null ? clientGroup.getUuid() : "null") +
                ", city=" + (city != null ? city.getIbgeCode() : "null") +
                ", createdAt=" + createdAt +
                '}';
    }
}