package br.com.groupsoftware.grouppay.extratoremail.repository;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.DocumentStageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório para a entidade {@link DocumentStageHistory}.
 *
 * Esta interface extende {@link JpaRepository} e fornece métodos padrão de CRUD e consultas específicas
 * para acessar e manipular os dados históricos de estagio de documentos no banco de dados.
 * <p>
 * O repositório inclui métodos para buscar o histórico de estagio de um documento específico.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Repository
public interface DocumentStageHistoryRepository extends JpaRepository<DocumentStageHistory, Long> {
    List<DocumentStageHistory> findByDocument(Document document);
}