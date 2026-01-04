package br.com.groupsoftware.grouppay.extratoremail.repository;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Regex;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.ExpenseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório para a entidade {@link Regex}.
 * <p>
 * Esta ‘interface’ extende {@link JpaRepository} e fornece métodos padrão de CRUD e consultas específicas
 * para acessar e manipular as expressões regulares relacionadas às despesas no banco de dados.
 * <p>
 * O repositório permite consultar as expressões regulares conforme o tipo de documento ( {@link ExpenseType} )
 * e subtipo associado.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Repository
public interface RegexRepository extends JpaRepository<Regex, Long> {
    List<Regex> findByExpenseType(ExpenseType expenseType);

    Regex findByExpenseTypeAndIbgeCode(ExpenseType expenseType, String ibgeCode);
}