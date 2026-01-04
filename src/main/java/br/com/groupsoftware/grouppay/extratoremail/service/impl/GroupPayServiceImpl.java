package br.com.groupsoftware.grouppay.extratoremail.service.impl;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.*;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.*;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.ClientGroupDTO;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.DocumentDTO;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.EmailSearchConfigDTO;
import br.com.groupsoftware.grouppay.extratoremail.exception.MailReaderException;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import br.com.groupsoftware.grouppay.extratoremail.service.EmailService;
import br.com.groupsoftware.grouppay.extratoremail.service.GroupPayService;
import br.com.groupsoftware.grouppay.extratoremail.service.MailService;
import br.com.groupsoftware.grouppay.extratoremail.service.S3DownloadService;
import br.com.groupsoftware.grouppay.extratoremail.util.password.Base64PasswordUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * Implementação do serviço {@link GroupPayService}.
 * <p>
 * Esta classe contém a lógica de negócio para processar documentos e gerenciar
 * grupos de clientes no sistema. É responsável por delegar as operações ao
 * repositório apropriado e garantir que as regras de negócio sejam aplicadas.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupPayServiceImpl implements GroupPayService {

    private final RepositoryFacade repository;
    private final ObjectMapper objectMapper;
    private final S3DownloadService s3Download;
    private final MailService mailService;
    private final EmailService emailService;
    private final String MENSAGEM_HASH = "[GROUP PAY] Hash de validação : d42ead812993e616c29147fac20c9fb7";

    @Override
    public void processExpense(DocumentDTO documentDTO) {
        log.info("Recebendo despesa: {}", documentDTO);
        Document document = repository.document.findById(documentDTO.getDocumentId()).orElseThrow(() ->
                new IllegalArgumentException("Documento não encontrado com ID: " + documentDTO.getDocumentId()));

        if (documentDTO.getAmazonPath() != null) {
            document.setStage(DocumentStage.SENT_TO_S3);
            document.setAmazonPath(documentDTO.getAmazonPath());
        } else {
            document.setStage(DocumentStage.DELETE_FROM_LOCAL);
        }

        document.addStageHistoryEntry();
        repository.document.save(document);
        log.info("Documento processado com sucesso: {}", document.getFileName());
    }

    @Override
    @Transactional
    public void createUpdateCompany(ClientGroupDTO clientGroupDTO) throws JsonProcessingException {
        log.info("[COMPANY_UPDATE] SALVANDO/ATUALIZANDO DADOS DE EMPRESA");
        String uuid = clientGroupDTO.getCompany().getUuid();
        Optional<City> city = repository.city.findByIbgeCode(clientGroupDTO.getCompany().getIbgeCode());
        Optional<Company> existingCompany = repository.company.findByUuid(uuid);
        Optional<ClientGroup> existingClientGroup = repository.clientGroup.findByUuid(clientGroupDTO.getUuid());

        //createCompanyBucketAndSesAlias(clientGroupDTO.getCompany().getEmail());

        Company company;
        if (existingCompany.isPresent()) {
            company = handleExistingCompany(existingCompany.get(), clientGroupDTO, existingClientGroup, city);
        } else {
            company = handleNewCompany(clientGroupDTO, existingClientGroup, city);
        }

        if (Objects.nonNull(company) && Objects.nonNull(clientGroupDTO.getEmailSearchConfig())) {
            log.info("[COMPANY_UPDATE] SALVANDO/ATUALIZANDO DADOS DE CONFIGURACAO DE EMAIL");
            EmailSearchConfigDTO emailSearchConfigDTO = clientGroupDTO.getEmailSearchConfig();

            Optional<EmailSearchConfig> emailSearchConfigSaved = repository.emailSearchConfig.findByUuid(emailSearchConfigDTO.getUuid());

            if (emailSearchConfigSaved.isPresent()) {
                handleExistingEmailSearchConfig(emailSearchConfigSaved.get(), emailSearchConfigDTO);
            }
            else {
                handleNewEmailSearchConfig(emailSearchConfigDTO, company);
            }
        }
    }

    @Override
    @Transactional
    public void createUpdateClientGroup(ClientGroupDTO clientGroupDTO) {
        log.info("[CREATE/UPDATE CLIENT GROUP] Sincronizando ClientGroup: UUID={}, CodigoSuporte={}", 
                clientGroupDTO.getUuid(), clientGroupDTO.getCodigoSuporte());
        
        Optional<ClientGroup> existingClientGroup = repository.clientGroup.findByUuid(clientGroupDTO.getUuid());
        
        if (existingClientGroup.isPresent()) {
            ClientGroup clientGroup = existingClientGroup.get();
            log.info("[CREATE/UPDATE CLIENT GROUP] Atualizando ClientGroup existente: UUID={}", clientGroupDTO.getUuid());
            
            if (clientGroupDTO.getUsername() != null) {
                clientGroup.setUsername(clientGroupDTO.getUsername());
            }
            if (clientGroupDTO.getCnpj() != null) {
                clientGroup.setCnpj(clientGroupDTO.getCnpj());
            }
            if (clientGroupDTO.getCodigoSuporte() != null) {
                clientGroup.setCodigoSuporte(clientGroupDTO.getCodigoSuporte());
            }
            
            repository.clientGroup.save(clientGroup);
            log.info("[CREATE/UPDATE CLIENT GROUP] ClientGroup atualizado com sucesso: UUID={}", clientGroupDTO.getUuid());
        } else {
            log.info("[CREATE/UPDATE CLIENT GROUP] Criando novo ClientGroup: UUID={}", clientGroupDTO.getUuid());
            ClientGroup newClientGroup = ClientGroup.builder()
                    .uuid(clientGroupDTO.getUuid())
                    .username(clientGroupDTO.getUsername())
                    .cnpj(clientGroupDTO.getCnpj())
                    .codigoSuporte(clientGroupDTO.getCodigoSuporte())
                    .aiPlanType(AiPlanType.COMPLETE)
                    .aiUser(true)
                    .build();
            repository.clientGroup.save(newClientGroup);
            log.info("[CREATE/UPDATE CLIENT GROUP] ClientGroup criado com sucesso: UUID={}", clientGroupDTO.getUuid());
        }
    }

    @Override
    @Transactional
    public void updateEmailClientGroup(String uuidClientGroup, String email) {
        ClientGroup clientGroup = repository.clientGroup.findByUuid(uuidClientGroup).orElseThrow();
        clientGroup.setEmail(email);
        repository.clientGroup.save(clientGroup);
    }

    @Async
    @Transactional(dontRollbackOn = { MessagingException.class, MailReaderException.class })
    public void sendMailTest(String codigoSuporte) throws MailReaderException, MessagingException {
        log.info("[SEND_MAIL_TEST] TESTANDO CONFIGURAÇÕES DE EMAIL");
        Optional<ClientGroup> codigoSuporteOp = repository.clientGroup.findByCodigoSuporte(codigoSuporte);

        if (codigoSuporteOp.isPresent()) {
            ClientGroup clientGroup = codigoSuporteOp.get();
            Optional<Company> company = clientGroup.getCompanies().stream().filter(Company::isActive).findFirst();
            EmailSearchConfig emailSearchConfig = company.map(Company::getEmailSearchConfig).orElse(null);

            if (Objects.nonNull(emailSearchConfig)) {
                if (ConfigurationEmailType.ACESSO_DIRETO_CAIXA.equals(emailSearchConfig.getConfigurationEmail())) {
                    if (!StringUtils.isBlank(emailSearchConfig.getEmail()) && !StringUtils.isBlank(emailSearchConfig.getPassword())
                            && !StringUtils.isBlank(emailSearchConfig.getServer()) && Objects.nonNull(emailSearchConfig.getPort())
                            && Objects.nonNull(emailSearchConfig.getCryptography()) && Objects.nonNull(emailSearchConfig.getProtocol())) {
                        testAccessMail(emailSearchConfig, clientGroup);
                    }
                }
                else if (ConfigurationEmailType.REDIRECIONAMENTO_ALIAS.equals(emailSearchConfig.getConfigurationEmail())) {
                    if (!StringUtils.isBlank(emailSearchConfig.getTestSendEmail())) {
                        testRedirectMail(clientGroup, emailSearchConfig);
                    }
                }
            }
            else {
                throw new MailReaderException("Configurações de email não encontradas.");
            }
        }
        else {
            throw new MailReaderException("Cliente Group não encontrado.");
        }
    }

    @Transactional
    public void enableDisableCompany(String uuidCompany, boolean enable) {
        Optional<Company> companyOp = repository.company.findByUuid(uuidCompany);

        if (companyOp.isPresent()) {
            Company company = companyOp.get();

            if (Objects.nonNull(company.getEmailSearchConfig())) {
                EmailSearchConfig emailSearchConfig = company.getEmailSearchConfig();
                emailSearchConfig.setActive(enable);
                repository.emailSearchConfig.save(emailSearchConfig);
            }

            company.setActive(enable);
            repository.company.save(company);
        }
    }

    private void testAccessMail(EmailSearchConfig emailSearchConfig, ClientGroup clientGroup) throws MailReaderException {
        try {
            validateEmailConfiguration(emailSearchConfig);

            boolean usarOAuth2MicrosftAzure = emailService.usarOAuth2MicrosftAzure(emailSearchConfig.getEmail());
            Properties properties = emailService.getEmailProperties(emailSearchConfig, usarOAuth2MicrosftAzure);

            try (Store store = emailService.connectToEmailStore(properties, emailSearchConfig, usarOAuth2MicrosftAzure)) {
                Folder inbox = store.getFolder("INBOX");
                inbox.open(Folder.READ_ONLY);
                inbox.getMessages();

                emailService.registerAccessLog(emailSearchConfig.getEmail(), ReasonAccessType.TEST_CONFIG);

                log.info("[SEND_MAIL_TEST] ATUALIZANDO STATUS DO TESTE DE ACESSO DO EMAIL");
                updateEmailSearchConfigCompanies(clientGroup, RedirectStatusTestType.CONCLUIDO, null, ZonedDateTime.now());
            }

        } catch (Exception e) {
            log.error("[SEND_MAIL_TEST] ERRO AO ACESSAR EMAIL: {}", e.getMessage(), e);

            String stacktrace = ExceptionUtils.getStackTrace(e);
            if (stacktrace.length() > 500) {
                stacktrace = stacktrace.substring(0, 500);
            }

            log.info("[SEND_MAIL_TEST] ATUALIZANDO STATUS DO TESTE DE ACESSO DO EMAIL COM ERRO");
            updateEmailSearchConfigCompanies(clientGroup, RedirectStatusTestType.ERRO, stacktrace, ZonedDateTime.now());
        }
    }

    private void testRedirectMail(ClientGroup clientGroup, EmailSearchConfig emailSearchConfig) throws MailReaderException {
        try {
            HashMap<String, Object> parameters = new HashMap<>();
            
            String emailRedirect = emailSearchConfig.getEmail();
            boolean isGmail = emailRedirect != null && 
                             emailRedirect.toLowerCase().trim().endsWith("@gmail.com");
            
            if (isGmail) {
                parameters.put("msgHash", MENSAGEM_HASH);
                log.info("[SEND_MAIL_TEST] Email Gmail detectado, incluindo hash de validação");
            } else {
                log.info("[SEND_MAIL_TEST] Email não-Gmail detectado, enviando sem hash de validação");
            }
            
            parameters.put("emailRedirect", emailRedirect);
            parameters.put("uuidClientGroup", clientGroup.getUuid());

            emailSearchConfig.setTestSendDateTime(ZonedDateTime.now());
            mailService.sendMail(emailSearchConfig.getTestSendEmail(), "Teste de redirecionamento de e-mail - Group Pay", "email_teste_redirecionamento.txt", parameters);


            RedirectStatusTestType status = isGmail ? RedirectStatusTestType.PENDENTE : RedirectStatusTestType.CONCLUIDO;
            updateEmailSearchConfigCompanies(clientGroup, status, null, ZonedDateTime.now());
        } catch (Exception e) {
            log.error("[SEND_MAIL_TEST] Erro ao enviar email de teste de redirecionamento: {}", e.getMessage(), e);
            String stacktrace = ExceptionUtils.getStackTrace(e);
            if (stacktrace.length() > 500) {
                stacktrace = stacktrace.substring(0, 500);
            }
            updateEmailSearchConfigCompanies(clientGroup, RedirectStatusTestType.ERRO, stacktrace, ZonedDateTime.now());
        }
    }

    private void validateEmailConfiguration(EmailSearchConfig emailSearchConfig) throws MailReaderException {
        String protocol = emailSearchConfig.getProtocol().getNome().toLowerCase();
        String server = emailSearchConfig.getServer().toLowerCase();
        
        if ("imap".equals(protocol)) {
            if (server.contains("smtp")) {
                log.error("[VALIDATE_EMAIL_CONFIG] Protocolo IMAP não pode usar servidor SMTP: {}", emailSearchConfig.getServer());
                throw new MailReaderException("Configuração incorreta: Protocolo IMAP requer servidor IMAP");
            }
            
            if (emailSearchConfig.getEmail() != null && emailSearchConfig.getEmail().toLowerCase().endsWith("@gmail.com")) {
                if (!server.contains("imap.gmail.com")) {
                    log.error("[VALIDATE_EMAIL_CONFIG] Gmail com IMAP requer servidor imap.gmail.com, mas foi informado: {}", emailSearchConfig.getServer());
                    throw new MailReaderException("Configuração incorreta para Gmail com IMAP");
                }
            }
        }
        
        if ("pop3".equals(protocol)) {
            if (server.contains("smtp")) {
                log.error("[VALIDATE_EMAIL_CONFIG] Protocolo POP3 não pode usar servidor SMTP: {}", emailSearchConfig.getServer());
                throw new MailReaderException("Configuração incorreta: Protocolo POP3 requer servidor POP3");
            }
        }
    }

    @Transactional
    private void updateEmailSearchConfigCompanies(ClientGroup clientGroup, RedirectStatusTestType status, String stacktrace, ZonedDateTime dateTime) throws MailReaderException {
        String emailConfiguracao = null;
        
        clientGroup.getCompanies().forEach(company -> {
            EmailSearchConfig emailSearchConfig = company.getEmailSearchConfig();
            if (company.isActive() && Objects.nonNull(emailSearchConfig)) {
                emailSearchConfig.setTestSendDateTime(dateTime);
                emailSearchConfig.setTestSendStatus(status);
                emailSearchConfig.setStacktrace(stacktrace);

                repository.emailSearchConfig.save(emailSearchConfig);
            }
        });

        Optional<EmailSearchConfig> emailSearchConfigOp = clientGroup.getCompanies().stream()
                .filter(Company::isActive)
                .map(Company::getEmailSearchConfig)
                .filter(Objects::nonNull)
                .findFirst();
        
        if (emailSearchConfigOp.isPresent()) {
            emailConfiguracao = emailSearchConfigOp.get().getEmail();
        } else {
            emailConfiguracao = clientGroup.getCompanies().getFirst().getEmail();
        }
        
        emailService.sendValidationStatusToGroupPayCore(clientGroup, emailConfiguracao, status);
    }

    private void handleExistingEmailSearchConfig(EmailSearchConfig emailSearchConfig, EmailSearchConfigDTO emailSearchConfigDTO) {
        String password = getEncodedPassword(emailSearchConfigDTO.getPassword());
        emailSearchConfig.setActive(emailSearchConfigDTO.isActive());
        emailSearchConfig.setConfigurationEmail(emailSearchConfigDTO.getConfigurationEmail());
        emailSearchConfig.setEmail(emailSearchConfigDTO.getEmail());
        emailSearchConfig.setPassword(password);
        emailSearchConfig.setProtocol(emailSearchConfigDTO.getProtocol());
        emailSearchConfig.setServer(emailSearchConfigDTO.getServer());
        emailSearchConfig.setPort(emailSearchConfigDTO.getPort());
        emailSearchConfig.setTestSendEmail(emailSearchConfigDTO.getTestSendEmail());
        emailSearchConfig.setCryptography(emailSearchConfigDTO.getCryptography());

        if (ConfigurationEmailType.REDIRECIONAMENTO_ALIAS.equals(emailSearchConfigDTO.getConfigurationEmail()) 
                && emailSearchConfig.getTestSendStatus() == null) {
            emailSearchConfig.setTestSendStatus(RedirectStatusTestType.PENDENTE);
            if (emailSearchConfig.getTestSendDateTime() == null) {
                emailSearchConfig.setTestSendDateTime(ZonedDateTime.now());
            }
        }

        repository.emailSearchConfig.save(emailSearchConfig);
    }

    private void handleNewEmailSearchConfig(EmailSearchConfigDTO emailSearchConfigDTO, Company company) {
        String password = getEncodedPassword(emailSearchConfigDTO.getPassword());
        EmailSearchConfig emailSearchConfig = EmailSearchConfig.builder()
                .uuid(emailSearchConfigDTO.getUuid())
                .active(emailSearchConfigDTO.isActive())
                .configurationEmail(emailSearchConfigDTO.getConfigurationEmail())
                .email(emailSearchConfigDTO.getEmail())
                .password(password)
                .protocol(emailSearchConfigDTO.getProtocol())
                .server(emailSearchConfigDTO.getServer())
                .port(emailSearchConfigDTO.getPort())
                .testSendDateTime(emailSearchConfigDTO.getTestSendDateTime())
                .testSendEmail(emailSearchConfigDTO.getTestSendEmail())
                .cryptography(emailSearchConfigDTO.getCryptography())
                .company(company)
                .build();

        if (ConfigurationEmailType.REDIRECIONAMENTO_ALIAS.equals(emailSearchConfigDTO.getConfigurationEmail())) {
            emailSearchConfig.setTestSendStatus(RedirectStatusTestType.PENDENTE);
            if (emailSearchConfig.getTestSendDateTime() == null) {
                emailSearchConfig.setTestSendDateTime(ZonedDateTime.now());
            }
        }

        repository.emailSearchConfig.save(emailSearchConfig);
    }

    private static String getEncodedPassword(String password) {
        if (StringUtils.isBlank(password)) {
            return null;
        }
        if (Base64PasswordUtil.isStringCodificadaEmBase64(password)) {
            return password;
        }
        return Base64PasswordUtil.encode(password);
    }

    private void createCompanyBucketAndSesAlias(String email) {
        s3Download.createBucketAndSesAliasForCompany(email);
    }

    private Company handleExistingCompany(Company company, ClientGroupDTO clientGroupDTO, Optional<ClientGroup> existingClientGroup, Optional<City> city) throws JsonProcessingException {
        if (company.getStatus().equals(Status.PROCESSING)) {
            return saveCompanyUpdate(company, clientGroupDTO);
        } else {
            return updateExistingCompany(company, clientGroupDTO, existingClientGroup, city);
        }
    }

    private Company saveCompanyUpdate(Company company, ClientGroupDTO clientGroupDTO) throws JsonProcessingException {
        UpdateCompany updateCompany = repository.companyUpdate.save(
                UpdateCompany.builder()
                        .company(company)
                        .json(objectMapper.writeValueAsString(clientGroupDTO))
                        .build()
        );

        return updateCompany.getCompany();
    }

    private Company handleNewCompany(ClientGroupDTO clientGroupDTO, Optional<ClientGroup> existingClientGroup, Optional<City> city) {
        if (city.isPresent()) {
            return createNewCompany(clientGroupDTO, existingClientGroup, city.get());
        } else {
            log.info("Cidade({}), não cadastrada no sistema.", clientGroupDTO.getCompany().getIbgeCode());
            return null;
        }
    }

    private Company updateExistingCompany(Company company, ClientGroupDTO clientGroupDTO, Optional<ClientGroup> existingClientGroup, Optional<City> city) {
        log.info("Atualizando empresa existente com UUID: {}", company.getUuid());

        // Atualiza os dados da empresa
        company.setEmail(clientGroupDTO.getCompany().getEmail());
        company.setCnpj(clientGroupDTO.getCompany().getCnpj());
        company.setFantasyName(clientGroupDTO.getCompany().getFantasyName());
        company.setLegalName(clientGroupDTO.getCompany().getLegalName());
        city.ifPresent(company::setCity);
        company.setActive(clientGroupDTO.getCompany().isActive());

        // Verifica ou cria o grupo de clientes e associa
        ClientGroup clientGroup = getOrCreateClientGroup(clientGroupDTO, existingClientGroup);
        company.setClientGroup(clientGroup);

        repository.company.save(company);
        log.info("Empresa atualizada com sucesso: {}", company);

        return company;
    }

    private Company createNewCompany(ClientGroupDTO clientGroupDTO, Optional<ClientGroup> existingClientGroup, City city) {
        log.info("Criando nova empresa com UUID: {}", clientGroupDTO.getCompany().getUuid());

        // Verifica ou cria o grupo de clientes
        ClientGroup clientGroup = getOrCreateClientGroup(clientGroupDTO, existingClientGroup);
        clientGroup.setAiPlanType(AiPlanType.COMPLETE);
        clientGroup.setAiUser(true);

        // Cria uma nova empresa
        Company newCompany = Company.builder()
                .uuid(clientGroupDTO.getCompany().getUuid())
                .email(clientGroupDTO.getCompany().getEmail())
                .cnpj(clientGroupDTO.getCompany().getCnpj())
                .city(city)
                .lastMailRead(LocalDateTime.now(ZoneId.systemDefault()))
                .clientGroup(clientGroup) // Associa ao grupo de clientes
                .active(clientGroupDTO.getCompany().isActive())
                .build();

        repository.company.save(newCompany);
        log.info("Nova empresa criada com sucesso: {}", newCompany);

        return newCompany;
    }

    private ClientGroup getOrCreateClientGroup(ClientGroupDTO clientGroupDTO, Optional<ClientGroup> existingClientGroup) {
        if (existingClientGroup.isPresent()) {
            log.info("Grupo de clientes encontrado com UUID: {}", clientGroupDTO.getUuid());
            return existingClientGroup.get();
        } else {
            log.info("Grupo de clientes não encontrado. Criando novo com UUID: {}", clientGroupDTO.getUuid());
            ClientGroup newClientGroup = ClientGroup.builder()
                    .uuid(clientGroupDTO.getUuid())
                    .username(clientGroupDTO.getUsername())
                    .cnpj(clientGroupDTO.getCnpj())
                    .aiPlanType(AiPlanType.COMPLETE)
                    .aiUser(true)
                    .codigoSuporte(clientGroupDTO.getCodigoSuporte())
                    .build();
            return repository.clientGroup.save(newClientGroup);
        }
    }
}
