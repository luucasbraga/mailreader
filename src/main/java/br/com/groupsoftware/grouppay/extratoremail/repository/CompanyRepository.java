package br.com.groupsoftware.grouppay.extratoremail.repository;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.ClientGroup;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Company;
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
 * Repositório para a entidade {@link Company}.
 * <p>
 * Esta interface extende {@link JpaRepository} e oferece métodos padrão de CRUD e consultas específicas
 * para acessar e manipular os dados de empresas no banco de dados.
 * <p>
 * O repositório inclui métodos para encontrar empresas por e-mail ou CNPJ, buscar as 5 primeiras empresas com base
 * na data de leitura do último e-mail, e realizar consultas personalizadas utilizando a anotação {@link Query}.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    @Query("SELECT c FROM Company c WHERE c.status = 'PROCESSING'")
    List<Company> findAllByStatusProcessing();

    @Query("SELECT c FROM Company c " +
            "WHERE c.active = true " +
            "  AND (c.status = 'NOT_PROCESSING' " +
            "  OR c.updatedAt > :minDateTime)" +
            "  AND NOT EXISTS (SELECT 1 FROM UpdateCompany cup " +
            "                  WHERE cup.company = c " +
            "                    AND cup.updateStatus = 'NOT_UPDATED') " +
            "ORDER BY c.updatedAt ASC")
    Page<Company> findCompaniesEligibleForEmailProcessing(@Param("minDateTime") LocalDateTime minDateTime, Pageable pageable);


    @Query("SELECT c FROM Company c " +
            "WHERE c.active = true " +
            "AND (c.status = 'NOT_PROCESSING' " +
            "OR c.updatedAt > :minDateTime)" +
            "AND NOT EXISTS (SELECT 1 FROM UpdateCompany cup " +
            "            WHERE cup.company = c " +
            "            AND cup.updateStatus = 'NOT_UPDATED') " +
            "ORDER BY c.updatedAt ASC")
    Page<Company> findCompaniesEligibleForS3Processing(@Param("minDateTime") LocalDateTime minDateTime, Pageable pageable);

    Optional<Company> findByUuid(String uuid);

    List<Company> findByClientGroup(ClientGroup clientGroup);

    Optional<Company> findByCnpj(String cnpj);
}
