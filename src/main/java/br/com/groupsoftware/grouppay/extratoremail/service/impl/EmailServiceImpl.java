package br.com.groupsoftware.grouppay.extratoremail.service.impl;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.ClientGroup;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Company;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.EmailAccessLog;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.EmailSearchConfig;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.EmailSearchTerm;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.ConfigurationEmailType;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentStage;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.ReasonAccessType;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.RedirectStatusTestType;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.ExpenseDTO;
import br.com.groupsoftware.grouppay.extratoremail.exception.MailReaderException;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import br.com.groupsoftware.grouppay.extratoremail.security.GroupPayTokenManager;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.S3ObjectDTO;
import br.com.groupsoftware.grouppay.extratoremail.service.DocumentService;
import br.com.groupsoftware.grouppay.extratoremail.service.EmailService;
import br.com.groupsoftware.grouppay.extratoremail.service.GroupPayService;
import br.com.groupsoftware.grouppay.extratoremail.service.MailService;
import br.com.groupsoftware.grouppay.extratoremail.service.MicrosoftOAuth2Service;
import br.com.groupsoftware.grouppay.extratoremail.service.S3DownloadService;
import br.com.groupsoftware.grouppay.extratoremail.util.GmailConfirmExecutor;
import br.com.groupsoftware.grouppay.extratoremail.util.RestUtil;
import br.com.groupsoftware.grouppay.extratoremail.util.file.FileUtils;
import br.com.groupsoftware.grouppay.extratoremail.util.password.Base64PasswordUtil;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.mail.*;
import javax.mail.internet.MimeUtility;
import javax.mail.search.*;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;

/**
 * Implementação do serviço de busca e processamento de e-mails.
 * <p>
 * Esta classe lida com a conexão a servidores de e-mail, busca de mensagens
 * com anexos em PDF, e armazenamento desses arquivos localmente para posterior
 * processamento.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
class EmailServiceImpl implements EmailService {

    @Value("${reader.dir}")
    private String readerDir;

    @Value("${reader.download}")
    private String readerDownload;

    @Value("${reader.max-attachments}")
    private int maxAttachment;

    @Value("${reader.timeout-connection}")
    private String timeoutConnection;

    @Value("${mailReaderInitialDate}")
    private LocalDateTime mailReaderInitialDate;

    private ExecutorService executor;
    private final RepositoryFacade repository;
    private final RestTemplate restTemplate;
    private final GroupPayTokenManager tokenManager;
    private final DocumentService documentService;
    private final S3DownloadService s3DownloadService;
    private final MailService mailService;
    private final MicrosoftOAuth2Service microsoftOAuth2Service;
    @Lazy
    @Autowired
    private GroupPayService groupPayService;

    @Value("${group-pay.core}")
    private String host;

    private static final String BASE_PATH = "/api/v1/configuracao-mail-reader";
    private static final String PATH_REDIRECT_VALIDATION_SUCCESS = "/validacao/redirect";


    @Override
    public void getEmailsAndSavePdfs(ClientGroup clientGroup) throws Exception {
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        searchAndProcessPdfs(clientGroup);
    }

    private void searchAndProcessPdfs(ClientGroup clientGroup) throws Exception {

        // Busca todas as companies ativas associadas ao ClientGroup
        List<Company> activeCompanies = clientGroup.getCompanies().stream()
                .filter(Company::isActive)
                .toList();

        if (activeCompanies.isEmpty()) {
            return;
        }


        // Processa emails para cada company que tenha configuração de email ativa
        for (Company company : activeCompanies) {
            Optional<EmailSearchConfig> emailConfigOp = repository.emailSearchConfig.findByCompanyUuid(company.getUuid());

            if (emailConfigOp.isPresent() && emailConfigOp.get().isActive() &&
                ConfigurationEmailType.ACESSO_DIRETO_CAIXA.equals(emailConfigOp.get().getConfigurationEmail())) {

                EmailSearchConfig emailConfig = emailConfigOp.get();
                log.info("Processando email para Company {} - Config: {}", company.getFantasyName(), emailConfig.getEmail());

                try {
                    boolean usarOAuth2MicrosftAzure = usarOAuth2MicrosftAzure(emailConfig.getEmail());
                    Properties properties = getEmailProperties(emailConfig, usarOAuth2MicrosftAzure);

                    try (Store store = connectToEmailStore(properties, emailConfig, usarOAuth2MicrosftAzure)) {
                        Folder folder = store.getFolder("INBOX");
                        folder.open(Folder.READ_ONLY);

                        // Busca emails usando lastMailRead do ClientGroup
                        Message[] messages = searchLimitedMail(folder, clientGroup.getLastMailRead());
                        processAttachments(messages, clientGroup);

                        folder.close(false);

                        registerAccessLog(emailConfig.getEmail(), ReasonAccessType.PROCESS_ATTACHMENTS);
                        log.info("Processamento concluído para Company {} - {} mensagens encontradas", company.getFantasyName(), messages.length);

                    } catch (MessagingException e) {
                        log.error("Erro ao conectar ou buscar e-mails para Company {}: {}", company.getFantasyName(), e.getMessage(), e);
                        throw e;
                    }
                } catch (Exception e) {
                    log.error("Erro ao processar emails para Company {}: {}", company.getFantasyName(), e.getMessage(), e);
                }
            } else {
                log.debug("Company {} não possui configuração de email ativa ou não é ACESSO_DIRETO_CAIXA", company.getFantasyName());
            }
        }
    }

    @Transactional
    @Override
    public void registerAccessLog(String email, ReasonAccessType reason) {
        EmailAccessLog emailAccessLog = EmailAccessLog.builder()
                .uuid(UUID.randomUUID().toString())
                .email(email)
                .reasonAccess(reason)
                .build();

        repository.emailAccessLog.save(emailAccessLog);
    }

    private Message[] searchLimitedMail(Folder folder, LocalDateTime dataInicio) throws MessagingException {
        if (Objects.isNull(dataInicio)) {
            dataInicio = mailReaderInitialDate;
        }


        // Converte LocalDateTime diretamente para Date para incluir precisão de hora
        Date dataRecebimento = Date.from(dataInicio.atZone(ZoneId.systemDefault()).toInstant());
        SearchTerm filtroData = new ReceivedDateTerm(ReceivedDateTerm.GE, dataRecebimento);
        SearchTerm filtroComposto = criarFiltroComposto(filtroData);

        Message[] messages = folder.search(filtroComposto);
        Arrays.sort(messages, Comparator.comparing(this::getMessageReceivedDate));


        return messages;
    }

    private Document processAttachment(BodyPart anexo, ClientGroup clientGroup, String messageId) {
        String fileName = "unknown";
        try {
            fileName = anexo.getFileName();
            log.debug("[DEBUG] Iniciando processamento do anexo {} da mensagem {}", fileName, messageId);
            Document document = saveAttachment(anexo, clientGroup, messageId);
            if (document == null) {
                log.debug("[DEBUG] Documento não foi criado (provavelmente duplicado), messageId: {}", messageId);
            } else {
                log.info("[DEBUG] Documento criado com sucesso: ID={}, messageId={}, fileName={}", document.getId(), messageId, fileName);
            }
            return document;
        } catch (Exception e) {
            log.error("[DEBUG] Erro ao processar o anexo {} da mensagem {}: {}", fileName, messageId, e.getMessage(), e);
        }
        return null;
    }

    private SearchTerm criarFiltroComposto(SearchTerm filtroData) {
        List<String> termsList = repository.emailSearchTerm.findAll().stream()
                .map(EmailSearchTerm::getTerm)
                .toList();
        SearchTerm[] termosBusca = termsList.stream()
                .map(SubjectTerm::new)
                .toArray(SearchTerm[]::new);

        if (termosBusca.length > 0) {
        }

        return new AndTerm(new OrTerm(termosBusca), filtroData);
    }

    private boolean isPdfAttachment(BodyPart bodyPart) throws MessagingException {
        try {
            String disposition = bodyPart.getDisposition();
            String fileName = decodeFileName(bodyPart.getFileName());
            String contentType = bodyPart.getContentType();

            // Verifica se é um anexo (ATTACHMENT) ou se tem filename (pode ser INLINE mas ainda ser anexo)
            boolean isAttachment = Part.ATTACHMENT.equalsIgnoreCase(disposition);
            boolean hasFileName = fileName != null && !fileName.trim().isEmpty();
            
            // Verifica se é PDF pelo nome do arquivo (após decodificação)
            boolean isPdfByName = fileName != null && fileName.toLowerCase().endsWith(".pdf");
            
            // Verifica se é PDF pelo Content-Type
            boolean isPdfByContentType = contentType != null && 
                    (contentType.toLowerCase().contains("application/pdf") || 
                     contentType.toLowerCase().contains("pdf"));

            // Considera como anexo PDF se:
            // 1. Tem disposition ATTACHMENT E é PDF, OU
            // 2. Tem filename E é PDF (mesmo que disposition seja null ou INLINE), OU
            // 3. Tem Content-Type PDF (para casos onde filename pode estar codificado)
            boolean isValidPdfAttachment = (isAttachment && isPdfByName) ||
                                          (hasFileName && isPdfByName) ||
                                          (isPdfByContentType && hasFileName);

            log.info("[DEBUG] Verificando anexo - Disposition: {}, FileName: {}, ContentType: {}, IsAttachment: {}, IsPdfByName: {}, IsPdfByContentType: {}, Resultado: {}",
                    disposition, fileName, contentType, isAttachment, isPdfByName, isPdfByContentType, isValidPdfAttachment);

            return isValidPdfAttachment;
        } catch (MessagingException e) {
            log.warn("[DEBUG] Erro ao verificar anexo: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("[DEBUG] Erro inesperado ao verificar anexo: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Decodifica o nome do arquivo que pode estar codificado em MIME (ex: =?UTF-8?B?...)
     */
    private String decodeFileName(String encodedFileName) {
        if (encodedFileName == null || encodedFileName.trim().isEmpty()) {
            return encodedFileName;
        }
        try {
            return MimeUtility.decodeText(encodedFileName);
        } catch (Exception e) {
            log.debug("Erro ao decodificar nome do arquivo '{}', usando original: {}", encodedFileName, e.getMessage());
            return encodedFileName;
        }
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex);
    }

    private Date getMessageReceivedDate(Message message) {
        try {
            return message.getReceivedDate();
        } catch (MessagingException e) {
            log.error("Erro ao obter data do e-mail: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Store connectToEmailStore(Properties properties, EmailSearchConfig emailSearchConfig, boolean usarOAuth2MicrosftAzure) throws MessagingException {
        String password;

        // Prioriza OAuth2 delegado (por usuário) se estiver habilitado
        if (emailSearchConfig.getOauth2Enabled() != null && emailSearchConfig.getOauth2Enabled()) {
            try {
                log.info("Usando OAuth2 delegado para email: {}", emailSearchConfig.getEmail());
                password = microsoftOAuth2Service.getValidAccessToken(emailSearchConfig);
            } catch (Exception e) {
                log.error("Erro ao obter token OAuth2 delegado: {}", e.getMessage(), e);
                throw new MessagingException("Falha ao obter token OAuth2 delegado. Pode ser necessário autorizar novamente: " + e.getMessage(), e);
            }
        } else if (usarOAuth2MicrosftAzure) {
            // Fallback para client credentials (antigo) - deprecado
            try {
                log.warn("Usando OAuth2 client credentials (deprecado) para email: {}", emailSearchConfig.getEmail());
                password = getAccessTokenOAuth2MicrosoftAzure();
            } catch (Exception e) {
                log.error("Erro ao obter token OAuth2 do Microsoft Azure: {}", e.getMessage(), e);
                throw new MessagingException("Falha ao obter token OAuth2 do Microsoft Azure: " + e.getMessage(), e);
            }
        } else {
            // Autenticação com senha (IMAP/POP3 tradicional)
            password = Base64PasswordUtil.decode(emailSearchConfig.getPassword());
        }
        
        String protocol = emailSearchConfig.getProtocol().getNome();
        String server = emailSearchConfig.getServer();
        Integer port = emailSearchConfig.getPort();
        String cryptography = emailSearchConfig.getCryptography() != null ? emailSearchConfig.getCryptography().toString() : "N/A";
        
        log.info("Conectando ao email - Protocolo: {}, Servidor: {}, Porta: {}, Criptografia: {}, Email: {}", 
                protocol, server, port, cryptography, emailSearchConfig.getEmail());

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailSearchConfig.getEmail(), password);
            }
        });

        Store store = session.getStore();
        store.connect(properties.getProperty("mail."+ protocol + ".host"), emailSearchConfig.getEmail(), password);
        return store;
    }

    @Override
    public Properties getEmailProperties(EmailSearchConfig emailConfig, boolean usarOAuth2MicrosftAzure) {
        Properties properties = new Properties();

        // PROTOCOLO (imap or pop3)
        String protocol = emailConfig.getProtocol().getNome() == null ? "imap" : emailConfig.getProtocol().getNome();
        properties.put("mail.store.protocol", protocol);

        // HOST E PORT
        properties.put("mail." + protocol + ".host", emailConfig.getServer());
        properties.put("mail." + protocol + ".port", emailConfig.getPort());

        // AUTH (SEMPRE TRUE)
        properties.put("mail." + protocol + ".auth", "true");

        // TIMEOUTS
        properties.put("mail." + protocol + ".connectiontimeout", timeoutConnection);
        properties.put("mail." + protocol + ".timeout", timeoutConnection);
        properties.put("mail." + protocol + ".writetimeout", timeoutConnection);

        // Habilita XOAUTH2 se OAuth2 delegado estiver habilitado OU se for conta Microsoft
        if ((emailConfig.getOauth2Enabled() != null && emailConfig.getOauth2Enabled()) || usarOAuth2MicrosftAzure) {
            properties.put("mail." + protocol + ".auth.mechanisms", "XOAUTH2");
        }

        // SSL/TLS
        switch (emailConfig.getCryptography()) {
            case SSL -> {
                properties.put("mail." + protocol + ".ssl.enable", "true");
                properties.put("mail." + protocol + ".socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                properties.put("mail." + protocol + ".socketFactory.fallback", "false");
                properties.put("mail." + protocol + ".socketFactory.port", emailConfig.getPort());
            }
            case TLS -> {
                properties.put("mail." + protocol + ".starttls.enable", "true");
                properties.put("mail." + protocol + ".starttls.required", "true");
                properties.put("mail." + protocol + ".ssl.enable", "false");
            }
        }
        return properties;
    }

    @Transactional
    private Document saveAttachment(BodyPart bodyPart, ClientGroup clientGroup, String messageId) throws Exception {
        log.debug("[DEBUG] Verificando duplicação para messageId {} e ClientGroup {}", messageId, clientGroup.getId());

        if (repository.document.existsByMessageIdAndClientGroup(messageId, clientGroup)) {
            log.warn("[DEBUG] Documento com messageId {} já existe para ClientGroup {}, evitando duplicação.",
                    messageId, clientGroup.getId());
            return null; // Retorna null para indicar que o documento já existe
        }

        String decodedFileName = decodeFileName(bodyPart.getFileName());
        log.debug("[DEBUG] Criando arquivo para anexo {} (decodificado: {})", bodyPart.getFileName(), decodedFileName);
        Path targetDirectory = Paths.get(readerDir, readerDownload);
        FileUtils.createDirectoryIfNotExists(targetDirectory);

        String fileExtension = getFileExtension(decodedFileName);
        String uniqueFileName = UUID.randomUUID() + fileExtension;
        Path filePath = targetDirectory.resolve(uniqueFileName);

        try (InputStream inputStream = bodyPart.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("[DEBUG] Anexo salvo fisicamente: {}", filePath);
        } catch (Exception e) {
            log.error("[DEBUG] Erro ao salvar o anexo em: {}", filePath, e);
            throw e;
        }

        // Verificação final antes de salvar (última chance de evitar duplicação)
        if (repository.document.existsByMessageIdAndClientGroup(messageId, clientGroup)) {
            log.warn("[DEBUG] Documento com messageId {} foi criado por outro thread durante o processamento. " +
                    "Removendo arquivo local duplicado: {}", messageId, filePath);
            try {
                Files.deleteIfExists(filePath);
            } catch (Exception e) {
                log.error("Erro ao remover arquivo duplicado: {}", filePath, e);
            }
            return null;
        }

        log.debug("[DEBUG] Criando entidade Document para messageId {}", messageId);
        // Documento criado sem Company - será definida após matching
        Document document = Document.builder()
                .clientGroup(clientGroup) // Novo campo necessário
                .fileName(uniqueFileName)
                .messageId(messageId)
                .stage(DocumentStage.DOWNLOADED).build();

        Document savedDocument = repository.document.save(document);
        log.info("[DEBUG] Documento persistido no banco: ID={}, messageId={}, fileName={}, stage={}",
                savedDocument.getId(), messageId, uniqueFileName, savedDocument.getStage());
        return savedDocument;
    }


    /**
     * Processa recursivamente as partes de um multipart, incluindo multiparts aninhados.
     * Isso é necessário porque alguns emails têm estrutura como:
     * - Multipart (mixed/alternative)
     *   - Multipart (related) - corpo do email
     *   - BodyPart - anexo PDF
     */
    private void processMultipartParts(Multipart multipart, String messageId, ClientGroup clientGroup,
                                      List<Future<Document>> futures, int[] pdfCount, int maxAttachment) {
        try {
            for (int i = 0; i < multipart.getCount() && pdfCount[0] < maxAttachment; i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                String partFileName = decodeFileName(bodyPart.getFileName());
                String partDisposition = bodyPart.getDisposition();
                String contentType = bodyPart.getContentType();
                
                log.info("[DEBUG] Verificando parte {} da mensagem {}: filename={}, disposition={}, contentType={}",
                        i, messageId, partFileName, partDisposition, contentType);

                try {
                    // Verifica se a parte é um multipart aninhado
                    Object content = bodyPart.getContent();
                    if (content instanceof Multipart nestedMultipart) {
                        log.info("[DEBUG] Parte {} é um multipart aninhado com {} partes, processando recursivamente", 
                                i, nestedMultipart.getCount());
                        processMultipartParts(nestedMultipart, messageId, clientGroup, futures, pdfCount, maxAttachment);
                    } else if (isPdfAttachment(bodyPart)) {
                        // É um anexo PDF válido
                        pdfCount[0]++; // Incrementa a contagem de PDFs
                        log.info("[DEBUG] Anexo PDF encontrado: {} na mensagem {}, iniciando processamento", partFileName, messageId);
                        futures.add(executor.submit(() -> processAttachment(bodyPart, clientGroup, messageId)));
                    } else {
                        log.info("[DEBUG] Parte {} não é anexo PDF válido - filename: {}, disposition: {}, contentType: {}", 
                                i, partFileName, partDisposition, contentType);
                    }
                } catch (Exception e) {
                    log.warn("[DEBUG] Erro ao processar parte {} da mensagem {}: {}", i, messageId, e.getMessage());
                    // Continua processando outras partes mesmo se uma falhar
                }
            }
        } catch (Exception e) {
            log.error("Erro ao processar partes do multipart: {}", e.getMessage(), e);
        }
    }

    private void processAttachments(Message[] messages, ClientGroup clientGroup) {
        List<Future<Document>> futures = new ArrayList<>();
        int[] pdfCount = {0}; // Contagem de PDFs processados
        LocalDateTime lastSentDate = null; // Para armazenar a última data de envio
        LocalDateTime lastMailRead = Objects.nonNull(clientGroup.getLastMailRead()) ? clientGroup.getLastMailRead() : mailReaderInitialDate;

        log.info("{} mensagens do ClientGroup {}", messages.length, clientGroup.getId());
        for (Message message : messages) {
            try {
                String messageId = message.getHeader("Message-ID")[0];
                log.info("[DEBUG] Processando mensagem ID: {} para ClientGroup {}", messageId, clientGroup.getId());

                // Verifica se já foi processada para este ClientGroup específico
                // Usa verificação mais precisa que considera o ClientGroup para evitar falsos positivos
                if (repository.document.existsByMessageIdAndClientGroup(messageId, clientGroup)) {
                    log.info("Mensagem {} já processada para ClientGroup {}, ignorando.", messageId, clientGroup.getId());
                    continue; // Ignora a mensagem atual se já processada
                }

                log.info("[DEBUG] Mensagem {} não foi processada anteriormente, verificando conteúdo", messageId);

                if (message.getContent() instanceof Multipart multipart) {
                    log.info("[DEBUG] Mensagem {} tem conteúdo multipart com {} partes", messageId, multipart.getCount());
                    // Processa cada parte do multipart (recursivamente para multiparts aninhados)
                    processMultipartParts(multipart, messageId, clientGroup, futures, pdfCount, maxAttachment);
                    lastSentDate = message.getSentDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    if (lastMailRead.isBefore(lastSentDate)) {
                        clientGroup.setLastMailRead(lastSentDate);
                        repository.clientGroup.save(clientGroup);
                    }
                } else {
                    log.info("[DEBUG] Mensagem {} não tem conteúdo multipart (tipo: {})", messageId, 
                            message.getContent() != null ? message.getContent().getClass().getSimpleName() : "null");
                }
            } catch (Exception e) {
                log.error("Erro ao listar anexos da mensagem: {}", e.getMessage());
            }

            // Verifica se atingiu o máximo de anexos para parar a iteração
            if (pdfCount[0] >= maxAttachment) {
                log.debug("[DEBUG] Atingido limite máximo de {} anexos, parando processamento", maxAttachment);
                break; // Sai do loop se o limite for atingido
            }
        }

        // Processa os futuros e coleta os resultados, mas não afeta a lógica principal
        log.debug("[DEBUG] Aguardando conclusão de {} tarefas de processamento de anexos", futures.size());
        int successfulDocuments = 0;
        int failedDocuments = 0;

        for (Future<Document> future : futures) {
            try {
                Document document = future.get(); // Aguarda a conclusão da tarefa
                if (document != null) {
                    successfulDocuments++;
                    log.debug("[DEBUG] Tarefa concluída com sucesso para documento ID {}", document.getId());
                } else {
                    failedDocuments++;
                    log.debug("[DEBUG] Tarefa concluída mas documento é null (possivelmente duplicado)");
                }
            } catch (InterruptedException | ExecutionException e) {
                failedDocuments++;
                log.error("[DEBUG] Erro ao obter resultado da tarefa: {}", e.getMessage());
            }
        }

        log.info("[DEBUG] Processamento concluído: {} documentos criados, {} falhas, total de tarefas: {}",
                successfulDocuments, failedDocuments, futures.size());
    }

    @PreDestroy
    public void shutdownExecutor() {
        try {
            executor.shutdown();
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    @Transactional
    public void saveSendRedirectSuccess(String uuidClientGroup, String email) {
        try {
            ClientGroup clientGroup = repository.clientGroup.findByUuid(uuidClientGroup).orElseThrow();
            boolean sentWithSuccess = sendValidationStatusToGroupPayCore(clientGroup, email, RedirectStatusTestType.CONCLUIDO);
            
            if (sentWithSuccess) {
                List<EmailSearchConfig> emailConfigList = repository.emailSearchConfig.findByClientGroupUuidAndEmail(uuidClientGroup, email);
                emailConfigList.forEach(e -> e.setTestSendStatus(RedirectStatusTestType.CONCLUIDO));
                repository.emailSearchConfig.saveAll(emailConfigList);
            } else {
                log.warn("Falha ao enviar status CONCLUIDO para o core");
            }
        } catch (Exception e) {
            log.error("Erro ao processar confirmação de redirect sucesso: {}", e.getMessage(), e);
        }
    }

    public boolean sendValidationStatusToGroupPayCore(ClientGroup clientGroup, String email, RedirectStatusTestType status) throws MailReaderException {
        String token = tokenManager.getToken(clientGroup);
        HttpHeaders headers = RestUtil.createAuthHeaders(token);
        HttpEntity<ExpenseDTO> request = new HttpEntity<>(null, headers);
        String url = RestUtil.buildUrl(host, BASE_PATH, PATH_REDIRECT_VALIDATION_SUCCESS);
        url = url.concat("?email=").concat(email).concat("&status=").concat(status.toString());

        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(url, request, Void.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return true;
            } else if (response.getStatusCode() == HttpStatus.UNAUTHORIZED || response.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.warn("Token expirado. Renovando token para cliente group: {}", clientGroup.getUuid());
                tokenManager.renewToken(clientGroup);
                return sendValidationStatusToGroupPayCore(clientGroup, email, status);
            } else {
                throw new MailReaderException("Erro ao enviar sucesso de redirect uuidClienteGroup: " + clientGroup.getUuid() +
                        " | email: " + email + " | Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Erro ao enviar status de validação: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean usarOAuth2MicrosftAzure(String email) {
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        if (domain.equals("gmail.com") || domain.equals("googlemail.com") || domain.startsWith("yahoo.")) {
            return false;
        }

        String url = "https://login.microsoftonline.com/common/UserRealm/" + email + "?api-version=1.0";
        JsonNode responseBody = restTemplate.getForObject(url, JsonNode.class);

        return responseBody.get("account_type").asText().toLowerCase().equals("managed");
    }

    @Override
    @Transactional
    public void processRedirectConfirmation(String codigoSuporte) throws MailReaderException {
        ClientGroup clientGroup = findClientGroupByCodigoSuporte(codigoSuporte);
        S3ObjectDTO s3Object = null;
        
        try {
            s3Object = downloadConfirmationFileFromS3(codigoSuporte);
            validateRecipientEmail(s3Object);
            
            String requesterEmail = extractRequesterEmail(s3Object);
            String confirmationUrl = extractAndValidateConfirmationUrl(s3Object, clientGroup);
            
            boolean isGmail = requesterEmail != null && !requesterEmail.isBlank() && 
                             requesterEmail.toLowerCase().trim().endsWith("@gmail.com");
            
            if (isGmail) {
                boolean confirmationSuccess = attemptAutomaticConfirmation(confirmationUrl, s3Object, clientGroup, requesterEmail, codigoSuporte);
                if (confirmationSuccess) {
                    sendConfirmationNotificationsAndUpdateStatus(clientGroup, requesterEmail, codigoSuporte, s3Object, confirmationUrl);
                    sendTestEmailAfterConfirmation(clientGroup, codigoSuporte);
                }
            } else {
                sendConfirmationNotificationsAndUpdateStatus(clientGroup, requesterEmail, codigoSuporte, s3Object, confirmationUrl);
                sendTestEmailAfterConfirmation(clientGroup, codigoSuporte);
            }
            
        } catch (MailReaderException e) {
            if (e.getMessage() != null && e.getMessage().contains("Nenhum arquivo encontrado no S3")) {
                try {
                    groupPayService.sendMailTest(codigoSuporte);
                } catch (Exception testException) {
                    log.error("Erro ao testar redirecionamento: {}", testException.getMessage());
                    handleConfirmationFailure(clientGroup, "Erro ao testar redirecionamento: " + testException.getMessage(), RedirectStatusTestType.ERRO);
                    throw new MailReaderException("Erro ao testar redirecionamento: " + testException.getMessage(), testException);
                }
            } else {
                log.error("Erro ao processar confirmação para código de suporte {}: {}", codigoSuporte, e.getMessage());
                handleConfirmationFailure(clientGroup, e.getMessage(), RedirectStatusTestType.ERRO);
                throw e;
            }
        } catch (Exception e) {
            log.error("Erro inesperado ao processar confirmação para código de suporte {}: {}", codigoSuporte, e.getMessage(), e);
            handleConfirmationFailure(clientGroup, "Erro geral: " + e.getMessage(), RedirectStatusTestType.ERRO);
            throw new MailReaderException("Erro ao processar confirmação de redirecionamento: " + e.getMessage(), e);
        } finally {
            cleanupS3Resources(s3Object, codigoSuporte);
        }
    }


    private ClientGroup findClientGroupByCodigoSuporte(String codigoSuporte) throws MailReaderException {
        return repository.clientGroup.findByCodigoSuporte(codigoSuporte)
                .orElseThrow(() -> {
                    log.error("ClientGroup não encontrado para código de suporte: {}", codigoSuporte);
                    return new MailReaderException("ClientGroup não encontrado para o código de suporte: " + codigoSuporte);
                });
    }

    private S3ObjectDTO downloadConfirmationFileFromS3(String codigoSuporte) throws MailReaderException {
        try {
            return s3DownloadService.downloadConfirmationFile(codigoSuporte);
        } catch (Exception e) {
            throw new MailReaderException("Erro ao fazer download do arquivo de confirmação do S3: " + e.getMessage(), e);
        }
    }


    private String validateRecipientEmail(S3ObjectDTO s3Object) throws MailReaderException {
        String recipientEmail = s3Object.getRecipientEmail();
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new MailReaderException("Destinatário de teste não configurado para o ClientGroup.");
        }
        return recipientEmail;
    }

    private String extractAndValidateConfirmationUrl(S3ObjectDTO s3Object, ClientGroup clientGroup) throws MailReaderException {
        try {
            return extractConfirmationUrl(s3Object);
        } catch (MailReaderException e) {
            handleConfirmationFailure(clientGroup, "Erro ao extrair URL de confirmação: " + e.getMessage(), RedirectStatusTestType.ERRO);
            throw new MailReaderException("Erro ao extrair URL de confirmação do email: " + e.getMessage(), e);
        }
    }

    private boolean attemptAutomaticConfirmation(String confirmationUrl, S3ObjectDTO s3Object, 
                                                 ClientGroup clientGroup, String recipientEmail, 
                                                 String codigoSuporte) throws MailReaderException {
        try {
            boolean confirmationSuccess = GmailConfirmExecutor.executarConfirmacao(confirmationUrl);
            
            if (!confirmationSuccess) {
                handleConfirmationFailure(clientGroup, recipientEmail, codigoSuporte, s3Object, confirmationUrl,
                        "Falha na confirmação automática. É necessário confirmar manualmente pelo email.",
                        RedirectStatusTestType.ERRO_CONFIRMACAO_AUTOMATICA);
                throw new MailReaderException("Falha na confirmação automática. Por favor, confirme manualmente pelo email e tente novamente.");
            }
            
            return true;
            
        } catch (MailReaderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao executar confirmação automática: {}", e.getMessage(), e);
            handleConfirmationFailure(clientGroup, recipientEmail, codigoSuporte, s3Object, confirmationUrl,
                    "Erro na confirmação automática: " + e.getMessage(),
                    RedirectStatusTestType.ERRO_CONFIRMACAO_AUTOMATICA);
            throw new MailReaderException("Erro na confirmação automática. Por favor, confirme manualmente pelo email e tente novamente: " + 
                    e.getMessage(), e);
        }
    }


    @Transactional
    private void sendConfirmationNotificationsAndUpdateStatus(ClientGroup clientGroup, String recipientEmail,
                                                              String codigoSuporte, S3ObjectDTO s3Object,
                                                              String confirmationUrl) throws MailReaderException {
        try {
            sendConfirmationEmail(recipientEmail, codigoSuporte, s3Object, confirmationUrl, true, null);
        } catch (Exception e) {
            log.error("Erro ao enviar email de notificação: {}", e.getMessage());
            handleConfirmationFailure(clientGroup, "Confirmação realizada, mas erro ao enviar email de notificação: " + e.getMessage(),
                    RedirectStatusTestType.ERRO);
            throw new MailReaderException("Confirmação realizada, mas erro ao enviar email de notificação: " + e.getMessage(), e);
        }
        
        updateEmailSearchConfigStatus(clientGroup, RedirectStatusTestType.CONCLUIDO, null);
    }


    private void sendTestEmailAfterConfirmation(ClientGroup clientGroup, String codigoSuporte) {
        try {
            groupPayService.sendMailTest(codigoSuporte);
        } catch (Exception e) {
            log.warn("Não foi possível enviar email de teste após confirmação: {}", e.getMessage());
        }
    }


    @Transactional
    private void handleConfirmationFailure(ClientGroup clientGroup, String recipientEmail, String codigoSuporte,
                                          S3ObjectDTO s3Object, String confirmationUrl, String failureReason,
                                          RedirectStatusTestType status) {
        updateEmailSearchConfigStatus(clientGroup, status, failureReason);
        
        if (recipientEmail != null && s3Object != null && confirmationUrl != null) {
            try {
                sendManualConfirmationRequiredEmail(recipientEmail, codigoSuporte, s3Object, confirmationUrl);
            } catch (Exception e) {
                log.error("Erro ao enviar email de confirmação manual: {}", e.getMessage());
            }
        }
    }


    @Transactional
    private void handleConfirmationFailure(ClientGroup clientGroup, String failureReason, RedirectStatusTestType status) {
        updateEmailSearchConfigStatus(clientGroup, status, failureReason);
    }


    private void cleanupS3Resources(S3ObjectDTO s3Object, String codigoSuporte) {
        // Primeiro, limpa o arquivo específico de confirmação (se existir)
        if (s3Object != null && s3Object.getS3Key() != null && !s3Object.getS3Key().isBlank()) {
            try {
                s3DownloadService.deleteConfirmationFile(s3Object.getS3Key());
            } catch (MailReaderException e) {
                log.warn("Erro ao excluir arquivo de confirmação do bucket S3: {}", e.getMessage());
            }
        }

        // Depois, limpa toda a pasta de confirmação da administradora
        if (codigoSuporte != null && !codigoSuporte.isBlank()) {
            try {
                s3DownloadService.deleteConfirmationFolder(codigoSuporte);
            } catch (MailReaderException e) {
                log.warn("Erro ao limpar pasta de confirmação da administradora {}: {}", codigoSuporte, e.getMessage());
            }
        }
    }

    @Transactional
    private void updateEmailSearchConfigStatus(ClientGroup clientGroup, RedirectStatusTestType status, String stacktrace) {
        clientGroup.getCompanies().forEach(company -> {
            EmailSearchConfig emailSearchConfig = company.getEmailSearchConfig();
            if (company.isActive() && Objects.nonNull(emailSearchConfig)) {
                emailSearchConfig.setTestSendStatus(status);
                if (stacktrace != null) {
                    String truncatedStacktrace = stacktrace.length() > 500 ? stacktrace.substring(0, 500) : stacktrace;
                    emailSearchConfig.setStacktrace(truncatedStacktrace);
                }
                repository.emailSearchConfig.save(emailSearchConfig);
            }
        });

        String email = clientGroup.getCompanies().stream()
                .filter(Company::isActive)
                .map(Company::getEmailSearchConfig)
                .filter(Objects::nonNull)
                .map(EmailSearchConfig::getEmail)
                .filter(Objects::nonNull)
                .filter(emailStr -> !emailStr.isBlank())
                .findFirst()
                .orElseGet(() -> {
                    return clientGroup.getCompanies().stream()
                            .filter(Company::isActive)
                            .map(Company::getEmail)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(null);
                });

        if (email != null) {
            try {
                sendValidationStatusToGroupPayCore(clientGroup, email, status);
            } catch (Exception e) {
                log.error("Erro ao enviar status para groupPayCore: {}", e.getMessage());
            }
        }
    }

    private static final java.util.regex.Pattern URL_PATTERN = java.util.regex.Pattern.compile("https?://[^\"'\\s]+", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern REQUESTER_EMAIL_PATTERN = java.util.regex.Pattern.compile("([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})\\s+pediu\\s+para\\s+encaminhar", java.util.regex.Pattern.CASE_INSENSITIVE);

    private String extractConfirmationUrl(S3ObjectDTO s3Object) throws MailReaderException {
        String content = new String(s3Object.getContent(), java.nio.charset.StandardCharsets.UTF_8);
        content = content.replaceAll("=\\r?\\n", "");
        content = content.replaceAll("\\r?\\n", " ");

        java.util.regex.Matcher matcher = URL_PATTERN.matcher(content);

        while (matcher.find()) {
            String url = matcher.group();
            if (url.contains("/mail/vf-")) {
                return url;
            }
        }

        throw new MailReaderException("No confirmation link found in the email content.");
    }

    private String extractRequesterEmail(S3ObjectDTO s3Object) throws MailReaderException {
        String content = new String(s3Object.getContent(), java.nio.charset.StandardCharsets.UTF_8);
        content = content.replaceAll("=\\r?\\n", "");
        content = content.replaceAll("\\r?\\n", " ");

        java.util.regex.Matcher matcher = REQUESTER_EMAIL_PATTERN.matcher(content);

        if (matcher.find()) {
            String requesterEmail = matcher.group(1);
            return requesterEmail;
        }

        log.warn("No requester email found in the email content. Using recipient email as fallback.");
        return s3Object.getRecipientEmail();
    }

    private void sendConfirmationEmail(String recipientEmail, String codigoSuporte, S3ObjectDTO s3Object,
                                       String confirmationUrl, boolean confirmationSuccess, String failureReason) throws MailReaderException {

        String subject = "Confirmação de redirecionamento - código suporte " + codigoSuporte;

        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("Status da confirmação automática: ")
                .append(confirmationSuccess ? "SUCESSO" : "FALHA")
                .append("\n");

        if (confirmationUrl != null) {
            bodyBuilder.append("URL utilizada: ").append(confirmationUrl).append("\n");
        }

        if (!confirmationSuccess && failureReason != null && !failureReason.isBlank()) {
            bodyBuilder.append("Motivo da falha: ").append(failureReason).append("\n");
        }

        mailService.sendMailWithAttachment(
                recipientEmail,
                subject,
                bodyBuilder.toString(),
                s3Object.getFileName(),
                s3Object.getContent(),
                s3Object.getContentType()
        );
    }

    private void sendManualConfirmationRequiredEmail(String recipientEmail, String codigoSuporte, S3ObjectDTO s3Object,
                                                      String confirmationUrl) {
        try {
            String subject = "Confirmação manual necessária - código suporte " + codigoSuporte;

            StringBuilder bodyBuilder = new StringBuilder();
            bodyBuilder.append("A confirmação automática do redirecionamento falhou.\n\n");
            bodyBuilder.append("Por favor, confirme manualmente o redirecionamento clicando no link abaixo:\n");
            if (confirmationUrl != null) {
                bodyBuilder.append(confirmationUrl).append("\n\n");
            }
            bodyBuilder.append("Após confirmar manualmente, chame novamente o endpoint de confirmação para finalizar o processo.\n");

            mailService.sendMailWithAttachment(
                    recipientEmail,
                    subject,
                    bodyBuilder.toString(),
                    s3Object.getFileName(),
                    s3Object.getContent(),
                    s3Object.getContentType()
            );
        } catch (Exception e) {
            log.error("Erro ao enviar email de confirmação manual: {}", e.getMessage());
        }
    }

    public List<EmailAccessLog> getEmailAccessLogsByEmail(String email) {
        return repository.emailAccessLog.findByEmailOrderByCreatedAtDesc(email);
    }

    private String getAccessTokenOAuth2MicrosoftAzure() {
        String url = "https://login.microsoftonline.com/11d42f28-5201-4d3f-929c-83122fa1ee8d/oauth2/v2.0/token";

        // Cabeçalhos
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Payload (x-www-form-urlencoded)
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", "26bb43d4-62eb-4014-9eea-b34f48542b55");
        formData.add("scope", "https://outlook.office365.com/.default");
        formData.add("grant_type", "client_credentials");

        // Monta a requisição
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        // Faz a chamada POST
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException();
        }

        return response.getBody().get("access_token").asText();
    }
}
