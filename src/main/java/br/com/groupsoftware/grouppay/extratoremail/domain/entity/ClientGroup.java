package br.com.groupsoftware.grouppay.extratoremail.domain.entity;

import br.com.groupsoftware.grouppay.extratoremail.domain.enums.AiPlanType;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.Status;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidade que representa uma administradora de empresas no sistema.
 * <p>
 * Com a nova arquitetura, o ClientGroup centraliza o recebimento de emails
 * de todos os condomínios (Companies) associados. Armazena informações
 * relacionadas ao grupo de clientes, como identificador único (UUID),
 * token de acesso, status de usuário de IA, código de suporte e
 * configurações de email.
 * </p>
 *
 * @author Marco Willy
 * @version 2.0 - Atualizada para centralização de emails
 * @since 2024
 */

@Entity
@Table(name = "tb_client_group")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientGroup implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "UUID", unique = true, nullable = false)
    private String uuid;

    @Column(name = "USERNAME")
    private String username;

    @Column(name = "CNPJ", unique = true, nullable = false)
    private String cnpj;

    @Column(name = "TOKEN", columnDefinition = "LONGTEXT")
    private String token;

    @Column(name = "AI_USER")
    private boolean aiUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "AI_PLAN_TYPE")
    private AiPlanType aiPlanType;

    @Column(name = "CODIGO_SUPORTE", unique = true, nullable = false)
    private String codigoSuporte;

    // Novos campos para centralização de emails
    @Column(name = "EMAIL", unique = true)
    private String email;

    @Column(name = "LAST_MAIL_READ")
    private LocalDateTime lastMailRead;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private Status status;

    // Relacionamento com as companies
    @OneToMany(mappedBy = "clientGroup")
    @JsonIgnore
    private List<Company> companies;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = Status.NOT_PROCESSING;
        }
        if (this.lastMailRead == null) {
            this.lastMailRead = LocalDateTime.now();
        }
    }

    @Override
    public String toString() {
        return "ClientGroup{" +
                "id=" + id +
                ", uuid='" + uuid + '\'' +
                ", username='" + username + '\'' +
                ", cnpj='" + cnpj + '\'' +
                ", email='" + email + '\'' +
                ", aiUser=" + aiUser +
                ", aiPlanType=" + aiPlanType +
                ", codigoSuporte='" + codigoSuporte + '\'' +
                ", status=" + status +
                ", lastMailRead=" + lastMailRead +
                ", createdAt=" + createdAt +
                '}';
    }
}