package br.com.groupsoftware.grouppay.extratoremail.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

/**
 * Representa os termos de busca utilizados no serviço de e-mail.
 * Cada termo é armazenado como um registro único na tabela `tb_email_search_term`.
 *
 * <p>Utilizada para definir palavras ou frases que serão usadas para filtrar e buscar
 * mensagens específicas nos serviços de e-mail integrados.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Entity
@Table(name = "tb_email_search_term")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailSearchTerm implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TERM", nullable = false, unique = true)
    private String term;

    @Override
    public String toString() {
        return "EmailSearchTerm{" +
                "id=" + id +
                ", term='" + term + '\'' +
                '}';
    }
}