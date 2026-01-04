package br.com.groupsoftware.grouppay.extratoremail.repository;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.UpdateCompany;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.CompanyUpdateStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório responsável por operações de acesso a dados da entidade {@link UpdateCompany}.
 *
 * <p>Estende {@link JpaRepository} para fornecer métodos básicos de persistência,
 * como salvar, deletar e buscar registros. Além disso, inclui um método personalizado
 * para buscar todas as atualizações de empresas com um determinado status.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Repository
public interface UpdateCompanyRepository extends JpaRepository<UpdateCompany, Long> {
    List<UpdateCompany> findAllByUpdateStatus(CompanyUpdateStatus companyUpdateStatus, Pageable pageable);
}

