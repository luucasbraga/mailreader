package br.com.groupsoftware.grouppay.extratoremail.repository;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.ClientGroup;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import java.time.LocalDateTime;

/**
 * Repositório para a entidade {@link Document}.
 * <p>
 * Esta interface extende {@link JpaRepository} e oferece métodos padrão de CRUD e consultas específicas
 * para acessar e manipular os dados de documentos no banco de dados.
 * <p>
 * O repositório inclui métodos para buscar documentos com estagios específicos, verificar a existência de documentos
 * com base no ID da mensagem, e realizar consultas personalizadas utilizando a anotação {@link Query}.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findAllByStageIn(List<DocumentStage> stageList);

    List<Document> findAllByStage(DocumentStage stageList);

    @Query("SELECT d FROM Document d JOIN d.stagesHistory sh WHERE sh.stage = :stage")
    List<Document> findDocumentsByStagesInHistory(@Param("stage") DocumentStage stage);

    @Query("SELECT d FROM Document d WHERE d.stage NOT IN :excludedStages " +
            " AND d.id IN (" +
            "SELECT DISTINCT dl.document.id FROM DocumentStageHistory dl " +
            "WHERE dl.stage NOT IN :excludedStages)")
    List<Document> findDocumentsInHistoryExcludingStages(
            @Param("excludedStages") List<DocumentStage> excludedStages);

    @Query("SELECT d FROM Document d WHERE d.stage = :stage " +
            "AND d.status = 'NOT_PROCESSING' " +
            "AND (d.updatedAt IS NULL OR d.updatedAt < :minDateTime)")
    List<Document> findDocumentsEligibleForProcessing(
            @Param("stage") DocumentStage stage,
            @Param("minDateTime") LocalDateTime minDateTime);

    List<Document> findByClientGroupAndStage(ClientGroup clientGroup, DocumentStage stage);

    List<Document> findByClientGroupAndCompanyIsNull(ClientGroup clientGroup);

    boolean existsByMessageId(String messageId);

    @Query("SELECT COUNT(d) > 0 FROM Document d WHERE d.messageId = :messageId AND d.clientGroup = :clientGroup")
    boolean existsByMessageIdAndClientGroup(@Param("messageId") String messageId, @Param("clientGroup") ClientGroup clientGroup);

    boolean existsByTextExtracted(String textExtracted);
}