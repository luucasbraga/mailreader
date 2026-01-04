package br.com.groupsoftware.grouppay.extratoremail.repository;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para a entidade {@link City}.
 *
 * Esta interface extende {@link JpaRepository}, fornecendo métodos padrão de CRUD e consultas específicas
 * para acessar e manipular os dados dos municípios armazenados no banco de dados.
 * <p>
 * O repositório oferece funcionalidades para consultar os municípios por nome, código IBGE e código de UF (Unidade da Federação).
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Repository
public interface CityRepository extends JpaRepository<City, Integer> {
    City findByName(String nome);

    Optional<City> findByIbgeCode(Long ibgeCode);

    List<City> findByStateCodeUf(Integer codigoUf);
}
