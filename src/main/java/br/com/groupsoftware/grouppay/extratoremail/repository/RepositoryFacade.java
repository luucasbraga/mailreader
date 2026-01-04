package br.com.groupsoftware.grouppay.extratoremail.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Facade para centralizar o acesso aos repositórios da aplicação.
 * <p>
 * Esta classe fornece uma interface simplificada para acessar diferentes repositórios,
 * facilitando a injeção e a reutilização em serviços e componentes. O objetivo é centralizar
 * e unificar o acesso aos repositórios, tornando o código mais organizado e mantendo a
 * consistência nas operações de persistência.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Component
@RequiredArgsConstructor
public class RepositoryFacade {
    public final ClientGroupRepository clientGroup;
    public final CompanyRepository company;
    public final DocumentRepository document;
    public final DocumentStageHistoryRepository documentStageHistory;
    public final EmailSearchTermRepository emailSearchTerm;
    public final StateRepository state;
    public final CityRepository city;
    public final RegexRepository regex;
    public final AIMessageModelRepository aiMessageModel;
    public final UserRepository user;
    public final UpdateCompanyRepository companyUpdate;
    public final EmailSearchConfigRepository emailSearchConfig;
    public final EmailAccessLogRepository emailAccessLog;
}
