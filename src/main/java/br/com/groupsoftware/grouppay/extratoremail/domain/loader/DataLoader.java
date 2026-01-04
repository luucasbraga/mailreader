package br.com.groupsoftware.grouppay.extratoremail.domain.loader;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.ClientGroup;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Company;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.User;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.AiPlanType;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import br.com.groupsoftware.grouppay.extratoremail.repository.UserRepository;
import br.com.groupsoftware.grouppay.extratoremail.util.password.Base64PasswordUtil;
import br.com.groupsoftware.grouppay.extratoremail.util.password.PasswordUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Componente responsável por carregar dados padrão no sistema, como modelos de mensagem JSON, termos de busca de e-mail,
 * configurações de e-mail, dados de estado e cidade, grupos de clientes e empresas, e expressões regulares para documentos fiscais.
 * <p>
 * Este componente é executado automaticamente após a inicialização da aplicação para garantir que dados essenciais estejam disponíveis
 * para o funcionamento do sistema.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Component
@RequiredArgsConstructor
public class DataLoader {

    private final RepositoryFacade repository;
    private final UserRepository userRepository;
    private final Environment environment;

    @PostConstruct
    @Transactional
    public void loadData() throws NoSuchAlgorithmException {

        // Inserir os modelos de mensagem JSON se a tabela estiver vazia
        if (userRepository.findAll().isEmpty()) {
            String salt = PasswordUtil.generateSalt();
            String hashedPassword = PasswordUtil.hashPassword("readerGrouppay@2024", salt);

            User user = new User();
            user.setUsername("grouppay");
            user.setPasswordHash(hashedPassword);
            user.setSalt(salt);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            repository.user.save(user);
        }
        // Verifica e carrega os grupos de clientes e empresas padrão
        if (false && repository.clientGroup.findAll().isEmpty() && Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
            ClientGroup clientGroup = ClientGroup.builder()
                    .uuid(UUID.randomUUID().toString())
                    .aiUser(false)
                    .username("usuario.group@gp_administradora1_6")
                    .codigoSuporte("11")
                    .cnpj("95035765000111")
                    .aiPlanType(AiPlanType.COMPLETE)
                    .build();
            repository.clientGroup.save(clientGroup);

            ClientGroup clientGroup1 = ClientGroup.builder()
                    .uuid(UUID.randomUUID().toString())
                    .aiUser(false)
                    .username("usuario.group@gp_teste_45")
                    .aiPlanType(AiPlanType.COMPLETE)
                    .codigoSuporte("TRIAL_67a56c3c-2c62-4a61-b10f-179c86beef0e")
                    .cnpj("37606305000151")
                    .build();

            Company company = Company.builder()
                    .email("fbbnenas@gmail.com")
                    .clientGroup(clientGroup)
                    .active(true)
                    .uuid("303e6499-90e5-475c-9d2a-5a57e702be28")
                    .city(repository.city.findByIbgeCode(3106200L).get())
                    .cnpj("12345678901234")
                    .lastMailRead(LocalDate.of(2024, 1, 1).atStartOfDay())
                    .build();


            Company company1 = Company.builder()
                    .email("marcowillyazevedo@gmail.com")
                    .clientGroup(clientGroup1)
                    .uuid("52208aeb-4d9f-4e34-b0bf-c1004a5c08ca")
                    .active(true)
                    .city(repository.city.findByIbgeCode(3106200L).get())
                    .cnpj("12345678901235")
                    .lastMailRead(LocalDate.of(2024, 1, 1).atStartOfDay())
                    .build();

            clientGroup.setCompanies(List.of(company));
            clientGroup1.setCompanies(List.of(company1));
            repository.clientGroup.saveAll(List.of(clientGroup, clientGroup1));
            repository.company.saveAll(Arrays.asList(company, company1));
        }
    }
}
