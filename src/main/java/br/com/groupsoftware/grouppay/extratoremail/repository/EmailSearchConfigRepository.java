package br.com.groupsoftware.grouppay.extratoremail.repository;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.EmailSearchConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailSearchConfigRepository extends JpaRepository<EmailSearchConfig, Long> {

    Optional<EmailSearchConfig> findByUuid(String uuid);

    Optional<EmailSearchConfig> findByCompanyUuid(String companyUuid);

    @Query("FROM EmailSearchConfig e " +
            " WHERE e.company.clientGroup.uuid = :uuidClientGroup " +
            " AND e.email = :email ")
    List<EmailSearchConfig> findByClientGroupUuidAndEmail(
            @Param("uuidClientGroup") String uuidClientGroup,
            @Param("email") String email);

}
