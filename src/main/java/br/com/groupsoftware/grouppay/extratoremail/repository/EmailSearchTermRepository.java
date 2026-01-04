package br.com.groupsoftware.grouppay.extratoremail.repository;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.EmailSearchTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório para a entidade {@link EmailSearchTerm}.
 *
 * Esta interface extende {@link JpaRepository} e fornece métodos padrão de CRUD e consultas específicas
 * para acessar e manipular os termos de busca de e-mail no banco de dados.
 * <p>
 * O repositório inclui um método para verificar se um termo de busca específico já existe.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Repository
public interface EmailSearchTermRepository extends JpaRepository<EmailSearchTerm, Long> {
    boolean existsByTerm(String term);
}