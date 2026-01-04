package br.com.groupsoftware.grouppay.extratoremail.repository;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.AIMessageModel;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.AiPlanType;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.ExpenseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório para a entidade {@link AIMessageModel}.
 *
 * Esta interface fornece métodos para acessar e manipular os dados relacionados aos modelos de mensagens
 * para diferentes tipos de despesas e planos de IA. Herda da interface {@link JpaRepository}, permitindo
 * a persistência e recuperação de dados de forma simplificada.
 *
 * <p>O método {@link #findByExpenseTypeAndPlanType(ExpenseType, AiPlanType)} permite buscar um modelo de mensagem
 * específico com base no tipo de despesa e no plano de IA associado.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Repository
public interface AIMessageModelRepository extends JpaRepository<AIMessageModel, Long> {
    AIMessageModel findByExpenseTypeAndPlanType(ExpenseType expenseType, AiPlanType planType);
}