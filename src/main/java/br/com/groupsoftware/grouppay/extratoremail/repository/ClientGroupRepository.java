package br.com.groupsoftware.grouppay.extratoremail.repository;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.ClientGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para a entidade {@link ClientGroup}.
 * <p>
 * Esta interface extende {@link JpaRepository}, oferecendo métodos padrão de CRUD e consultas específicas
 * para acessar e manipular os dados do grupo de clientes armazenados no banco de dados.
 * </p>
 * <p>
 * Com a nova arquitetura, o repositório inclui métodos específicos para buscar ClientGroups
 * elegíveis para processamento de emails, considerando status e última leitura de email.
 * </p>
 *
 * @author Marco Willy
 * @version 2.0 - Atualizado para arquitetura centralizada de emails
 * @since 2024
 */
@Repository
public interface ClientGroupRepository extends JpaRepository<ClientGroup, Long> {

    Optional<ClientGroup> findByUuid(String uuid);

    @Query("SELECT DISTINCT cg FROM ClientGroup cg " +
            "LEFT JOIN FETCH cg.companies c " +
            "WHERE cg.status = 'NOT_PROCESSING' " +
            "AND (cg.lastMailRead IS NULL OR cg.lastMailRead < :minDateTime)")
    Page<ClientGroup> findClientGroupsEligibleForEmailProcessing(
            @Param("minDateTime") LocalDateTime minDateTime, Pageable pageable);

    @Query("SELECT COUNT(cg) FROM ClientGroup cg WHERE cg.status = 'PROCESSING'")
    int countAllByStatusProcessing();

    Optional<ClientGroup> findByCodigoSuporte(String codigoSuporte);
}