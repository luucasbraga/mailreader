package br.com.groupsoftware.grouppay.extratoremail.domain.entity;

import br.com.groupsoftware.grouppay.extratoremail.domain.enums.CompanyUpdateStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Representa uma atualização de uma empresa no sistema.
 * <p>
 * Essa entidade armazena um JSON contendo os dados da empresa a serem atualizados,
 * além do status da atualização e a referência à empresa associada. Cada atualização
 * é registrada com um timestamp de criação.
 * </p>
 *
 * <p>
 * A entidade {@link UpdateCompany} é utilizada para controlar o histórico de atualizações
 * de uma empresa, garantindo que apenas empresas com status adequado sejam processadas.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */

@Entity
@Table(name = "tb_update_company")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCompany implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "COMPANY_ID")
    private Company company;

    @Column(name = "JSON")
    private String json;

    @Enumerated(EnumType.STRING)
    @Column(name = "UPDATE_STATUS")
    private CompanyUpdateStatus updateStatus;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.updateStatus == null) {
            this.updateStatus = CompanyUpdateStatus.NOT_UPDATED;
        }
    }

    @Override
    public String toString() {
        return "CompanyUpdate{" +
                "id=" + id +
                ", company=" + company.getId() +
                ", json='" + json + '\'' +
                ", updateStatus=" + updateStatus +
                ", createdAt=" + createdAt +
                '}';
    }

}