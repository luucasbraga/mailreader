package br.com.groupsoftware.grouppay.extratoremail.repository;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório responsável por operações de acesso a dados da entidade {@link User}.
 *
 * <p>Estende {@link JpaRepository} para fornecer métodos básicos de persistência,
 * como salvar, deletar e buscar entidades. Inclui um método personalizado para
 * buscar um usuário pelo nome de usuário.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}

