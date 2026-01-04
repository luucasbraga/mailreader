package br.com.groupsoftware.grouppay.extratoremail.repository;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.EmailAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailAccessLogRepository extends JpaRepository<EmailAccessLog, Long> {

    @Query("SELECT e FROM EmailAccessLog e WHERE e.email = :email ORDER BY e.createdAt DESC")
    List<EmailAccessLog> findByEmailOrderByCreatedAtDesc(@Param("email") String email);

}
