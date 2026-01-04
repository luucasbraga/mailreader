package br.com.groupsoftware.grouppay.extratoremail.repository;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório para a entidade {@link State}.
 * <p>
 * Extende {@link JpaRepository} para fornecer métodos padrão de CRUD e consultas específicas.
 * Armazena informações relacionadas aos estados, permitindo consultas por UF, nome e código do estado.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Repository
public interface StateRepository extends JpaRepository<State, Integer> {
    State findByUf(String uf);

    State findByName(String nome);

    State findByCodeUf(int i);
}
