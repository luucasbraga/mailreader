package br.com.groupsoftware.grouppay.extratoremail.service.impl;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Company;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.ClientGroup;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.ConfigurationEmailType;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentStage;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.Status;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.S3ObjectDTO;
import br.com.groupsoftware.grouppay.extratoremail.exception.MailReaderException;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import br.com.groupsoftware.grouppay.extratoremail.service.S3DownloadService;
import br.com.groupsoftware.grouppay.extratoremail.util.file.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.http.MediaType;
import jakarta.transaction.Transactional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import java.util.List;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SesException;
import software.amazon.awssdk.services.ses.model.VerifyEmailIdentityRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação do serviço de download de arquivos do Amazon S3.
 *
 * <p>Esta classe fornece a funcionalidade para listar e fazer o download de arquivos
 * armazenados no Amazon S3. O método principal, {@link #downloadFiles(Company)},
 * busca os 5 primeiros arquivos em uma pasta específica do bucket S3, ordenados
 * por data, e realiza o download para um diretório local.</p>
 *
 * <p>O cliente S3 é configurado com credenciais e região específicas para acessar
 * o bucket designado.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Slf4j
@Service
public class S3DownloadServiceImpl implements S3DownloadService {

    private final S3Client s3Client;
    private final SesClient sesClient;
    private final String bucketName;
    private final RepositoryFacade repository;

    @Value("${reader.max-attachments}")
    private int maxAttachment;

    @Value("${reader.dir}")
    private String readerDir;

    @Value("${reader.download}")
    private String readerDownload;

    @Value("${email.redirect.domain:cond21cloud}")
    private String emailRedirectDomain;

    public S3DownloadServiceImpl(
            @Value("${aws.s3-download.bucket-name}") String bucketName,
            @Value("${aws.s3-download.access-key-id}") String accessKeyId,
            @Value("${aws.s3-download.secret-access-key}") String secretAccessKey,
            @Value("${aws.s3-download.region}") String region, RepositoryFacade repository) {

        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofMinutes(2))
                        .apiCallAttemptTimeout(Duration.ofSeconds(30))
                        .build())
                .build();

        this.sesClient = SesClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofMinutes(2))
                        .apiCallAttemptTimeout(Duration.ofSeconds(30))
                        .build())
                .build();

        this.bucketName = bucketName;
        this.repository = repository;
    }

    /**
     * Faz o download dos 5 primeiros arquivos de uma pasta ordenados por data.
     *
     * @param company Empresa associada ao processo
     */
    @Override
    public void downloadFiles(Company company) {
        // Prefixo da pasta no S3, com base na estrutura
        String folderPrefix = "attachments/" + company.getEmail().substring(0, company.getEmail().indexOf("@")) + "/";

        try {
            // Listar objetos no S3 na pasta do cliente
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(folderPrefix) // Define a pasta como prefixo
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

            // Obtém a última data de leitura registrada
            Instant lastReadDate = company.getClientGroup().getLastMailRead() != null
                    ? company.getClientGroup().getLastMailRead().atZone(ZoneId.systemDefault()).toInstant()
                    : Instant.MIN; // Caso não tenha valor, define o menor possível

            // Ordenar por data, filtrar e pegar os arquivos mais recentes
            List<S3Object> sortedObjects = listResponse.contents().stream()
                    .filter(obj -> obj.key().endsWith(".pdf")) // Apenas PDFs
                    .filter(obj -> obj.lastModified().isAfter(lastReadDate)) // Apenas arquivos mais recentes
                    .sorted(Comparator.comparing(S3Object::lastModified)) // Ordena pelo mais antigo
                    .limit(maxAttachment)
                    .toList();

            // Atualizar última data de leitura
            if (!sortedObjects.isEmpty()) {
                Instant lastModified = sortedObjects.get(sortedObjects.size() - 1).lastModified();
                log.info("Atualizando última data de leitura para {}", lastModified);

                // Converte o `Instant` para `LocalDateTime` usando o fuso horário do sistema
                company.setLastMailRead(lastModified.atZone(ZoneId.systemDefault()).toLocalDateTime());
            }

            // Baixar arquivos e processar
            for (S3Object s3Object : sortedObjects) {
                ClientGroup clientGroup = company.getClientGroup();
                if (!repository.document.existsByMessageIdAndClientGroup(s3Object.key(), clientGroup)) {
                    downloadFile(s3Object, company);
                } else {
                    log.debug("Arquivo S3 {} já processado para ClientGroup {}, ignorando.", 
                            s3Object.key(), clientGroup.getId());
                }
            }

        } catch (S3Exception | IOException e) {
            log.error("Erro ao acessar o bucket S3: {}", e.getMessage());
        } finally {
            company.setStatus(Status.NOT_PROCESSING);
            repository.company.save(company);
        }
    }

    @Transactional
    private Document downloadFile(S3Object s3Object, Company company) throws IOException {
        ClientGroup clientGroup = company.getClientGroup();
        String messageId = s3Object.key();

        if (repository.document.existsByMessageIdAndClientGroup(messageId, clientGroup)) {
            log.warn("Documento com messageId {} já existe para ClientGroup {}, evitando duplicação.", 
                    messageId, clientGroup.getId());
            return null; // Retorna null para indicar que o documento já existe
        }

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Object.key())
                .build();

        Path targetDirectory = Paths.get(readerDir, readerDownload);
        FileUtils.createDirectoryIfNotExists(targetDirectory);

        String fileExtension = getFileExtension(Paths.get(s3Object.key()).getFileName().toString());
        String uniqueFileName = UUID.randomUUID() + fileExtension;
        Path filePath = targetDirectory.resolve(uniqueFileName);

        try (ResponseInputStream<GetObjectResponse> responseStream = s3Client.getObject(getRequest)) {
            Files.copy(responseStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Arquivo baixado e salvo com sucesso: {}", filePath);
        } catch (Exception e) {
            log.error("Erro ao salvar o arquivo do S3 em: {}", filePath, e);
            throw new RuntimeException("Erro ao salvar o arquivo do S3", e);
        }

        // Verificação final antes de salvar (última chance de evitar duplicação)
        if (repository.document.existsByMessageIdAndClientGroup(messageId, clientGroup)) {
            log.warn("Documento com messageId {} foi criado por outro thread durante o processamento. " +
                    "Removendo arquivo local duplicado: {}", messageId, filePath);
            try {
                Files.deleteIfExists(filePath);
            } catch (Exception e) {
                log.error("Erro ao remover arquivo duplicado: {}", filePath, e);
            }
            return null;
        }


        Document document = Document.builder()
                .clientGroup(clientGroup)
                .fileName(uniqueFileName)
                .messageId(messageId) 
                .stage(DocumentStage.DOWNLOADED).build();

        repository.document.save(document);
        log.debug("Documento criado com sucesso: ID={}, messageId={}, fileName={}, clientGroup={}", 
                document.getId(), messageId, uniqueFileName, clientGroup.getId());
        return document;
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex);
    }

    @Override
    public S3ObjectDTO downloadConfirmationFile(String codigoSuporte) throws MailReaderException {
        log.info("Iniciando download de arquivo de confirmação para código de suporte {}", codigoSuporte);

        ClientGroup clientGroup = repository.clientGroup.findByCodigoSuporte(codigoSuporte)
                .orElseThrow(() -> new MailReaderException("ClientGroup não encontrado para o código de suporte: " + codigoSuporte));

        List<Company> companies = repository.company.findByClientGroup(clientGroup);

        String email = resolveEmail(clientGroup, companies, codigoSuporte);


        String nomePasta = gerarNomeDaPasta(clientGroup, codigoSuporte);
        String folderPrefix = "confirm/" + nomePasta + "/";
        String recipientEmail = resolveTestSendEmail(companies);

        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(folderPrefix)
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

            if (CollectionUtils.isEmpty(listResponse.contents())) {
                throw new MailReaderException("Nenhum arquivo encontrado no S3 para o caminho: " + folderPrefix);
            }

            S3Object s3Object = listResponse.contents().stream()
                    .filter(object -> !object.key().endsWith("/"))
                    .max(Comparator.comparing(S3Object::lastModified))
                    .orElseThrow(() -> new MailReaderException("Nenhum arquivo válido encontrado para o caminho: " + folderPrefix));

            return getObjectContent(s3Object, recipientEmail);
        } catch (MailReaderException e) {
            throw e;
        } catch (S3Exception e) {
            log.error("Erro S3 ao listar arquivos da pasta {}: {}", folderPrefix, e.getMessage());
            throw new MailReaderException("Erro ao acessar o bucket S3 para confirmação de redirecionamento.");
        }
    }

    private S3ObjectDTO getObjectContent(S3Object s3Object, String recipientEmail) throws MailReaderException {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Object.key())
                .build();

        try (ResponseInputStream<GetObjectResponse> responseStream = s3Client.getObject(getRequest)) {
            byte[] content = responseStream.readAllBytes();
            String fileName = Paths.get(s3Object.key()).getFileName().toString();
            String contentType = Optional.ofNullable(responseStream.response().contentType())
                    .filter(type -> !type.isBlank())
                    .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);

            log.info("Arquivo de confirmação {} baixado com sucesso.", fileName);

            return S3ObjectDTO.builder()
                    .fileName(fileName)
                    .content(content)
                    .contentType(contentType)
                    .recipientEmail(recipientEmail)
                    .s3Key(s3Object.key())
                    .build();
        } catch (IOException e) {
            log.error("Erro IO ao baixar o arquivo {}: {}", s3Object.key(), e.getMessage());
            throw new MailReaderException("Erro ao baixar o arquivo de confirmação no S3.");
        } catch (S3Exception e) {
            log.error("Erro S3 ao obter o arquivo {}: {}", s3Object.key(), e.getMessage());
            throw new MailReaderException("Erro ao obter o arquivo de confirmação no S3.");
        }
    }

    private String resolveEmail(ClientGroup clientGroup, List<Company> companies, String codigoSuporte) throws MailReaderException {

        boolean isRedirectAlias = companies.stream()
                .filter(Objects::nonNull)
                .filter(Company::isActive)
                .map(Company::getEmailSearchConfig)
                .filter(Objects::nonNull)
                .filter(emailSearchConfig -> emailSearchConfig.isActive())
                .anyMatch(emailSearchConfig -> ConfigurationEmailType.REDIRECIONAMENTO_ALIAS.equals(emailSearchConfig.getConfigurationEmail()));

        if (isRedirectAlias) {
            return gerarNomeDaPasta(clientGroup, codigoSuporte) ;
        }

        if (clientGroup.getEmail() != null && !clientGroup.getEmail().isBlank()) {
            return clientGroup.getEmail();
        }

        return companies.stream()
                .map(Company::getEmail)
                .filter(Objects::nonNull)
                .filter(email -> !email.isBlank())
                .findFirst()
                .orElseThrow(() -> new MailReaderException("Email não configurado para o ClientGroup: " + codigoSuporte));
    }


    private String resolveTestSendEmail(List<Company> companies) {
        return companies.stream()
                .filter(Objects::nonNull)
                .map(Company::getEmailSearchConfig)
                .filter(Objects::nonNull)
                .map(emailSearchConfig -> emailSearchConfig.getEmail())
                .filter(Objects::nonNull)
                .filter(email -> !email.isBlank())
                .findFirst()
                .orElse(null);
    }


    private String gerarNomeDaPasta(ClientGroup clientGroup, String codigoSuporte) {
        String username = clientGroup.getUsername();

        String tenantId = username;
        if (username.contains("@")) {
            tenantId = username.substring(username.indexOf("@") + 1);
        }

        String nomeNormalizado = tenantId;
        if (tenantId.startsWith("gp_")) {
            nomeNormalizado = tenantId.substring(3);
        }

        int lastUnderscoreIndex = nomeNormalizado.lastIndexOf("_");
        if (lastUnderscoreIndex > 0) {
            String possibleId = nomeNormalizado.substring(lastUnderscoreIndex + 1);
            if (possibleId.matches("\\d+")) {
                nomeNormalizado = nomeNormalizado.substring(0, lastUnderscoreIndex);
            }
        }

        String[] partes = nomeNormalizado.split(" ");
        String doisPrimeirosNomesNormalizados = nomeNormalizado;
        
        if (partes.length >= 2) {
            doisPrimeirosNomesNormalizados = partes[0] + partes[1];
        } else if (partes.length == 1) {
            doisPrimeirosNomesNormalizados = partes[0];
        }

        return doisPrimeirosNomesNormalizados + codigoSuporte;
    }


    private String removerAcentosECaracteresEspeciais(String texto) {
        if (texto == null) {
            return "";
        }
        String textoNormalizado = Normalizer.normalize(texto, Normalizer.Form.NFD);
        textoNormalizado = textoNormalizado.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return textoNormalizado.replaceAll("[^a-zA-Z0-9]", "");
    }

    @Override
    public void createBucketAndSesAliasForCompany(String email) {
        String newBucketName = formatBucketName(email);

        createS3BucketIfNotExists(newBucketName);
        verifyEmailIdentityIfNotExists(email);
    }

    private void createS3BucketIfNotExists(String bucketName) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            log.info("Bucket '{}' já existe.", bucketName);
        } catch (S3Exception e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            log.info("Bucket '{}' criado com sucesso.", bucketName);
        }
    }

    private void verifyEmailIdentityIfNotExists(String email) {
        try {
            sesClient.verifyEmailIdentity(VerifyEmailIdentityRequest.builder().emailAddress(email).build());
            log.info("Alias SES '{}' configurado com sucesso.", email);
        } catch (SesException e) {
            log.error("Erro ao configurar o alias SES '{}': {}", email, e.awsErrorDetails().errorMessage());
        }
    }


    private String formatBucketName(String email) {
        return email.substring(0, email.indexOf("@")).replaceAll("[^a-zA-Z0-9]", "-").toLowerCase();
    }

    @Override
    public void deleteConfirmationFile(String s3Key) throws MailReaderException {
        if (s3Key == null || s3Key.isBlank()) {
            log.warn("Chave S3 não fornecida para exclusão. Nenhuma ação será realizada.");
            return;
        }

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("Arquivo de confirmação {} excluído com sucesso do bucket S3.", s3Key);
        } catch (S3Exception e) {
            log.error("Erro S3 ao excluir o arquivo {}: {}", s3Key, e.getMessage());
            throw new MailReaderException("Erro ao excluir o arquivo de confirmação no S3: " + e.getMessage());
        }
    }

    @Override
    public void deleteConfirmationFolder(String codigoSuporte) throws MailReaderException {
        log.info("Iniciando limpeza da pasta de confirmação para código de suporte {}", codigoSuporte);

        ClientGroup clientGroup = repository.clientGroup.findByCodigoSuporte(codigoSuporte)
                .orElseThrow(() -> new MailReaderException("ClientGroup não encontrado para o código de suporte: " + codigoSuporte));

        String nomePasta = gerarNomeDaPasta(clientGroup, codigoSuporte);
        String folderPrefix = "confirm/" + nomePasta + "/";

        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(folderPrefix)
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

            if (CollectionUtils.isEmpty(listResponse.contents())) {
                log.info("Pasta {} já está vazia ou não existe.", folderPrefix);
                return;
            }

            List<String> keysToDelete = listResponse.contents().stream()
                    .map(S3Object::key)
                    .toList();

            if (keysToDelete.isEmpty()) {
                log.info("Nenhum arquivo encontrado na pasta {} para deletar.", folderPrefix);
                return;
            }

            List<ObjectIdentifier> objectIdentifiers = keysToDelete.stream()
                    .map(key -> ObjectIdentifier.builder().key(key).build())
                    .toList();

            DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(Delete.builder().objects(objectIdentifiers).build())
                    .build();

            DeleteObjectsResponse deleteResponse = s3Client.deleteObjects(deleteRequest);

            log.info("Pasta de confirmação {} limpa com sucesso. {} arquivos deletados.",
                    folderPrefix, deleteResponse.deleted().size());

        } catch (S3Exception e) {
            log.error("Erro S3 ao limpar a pasta {}: {}", folderPrefix, e.getMessage());
            throw new MailReaderException("Erro ao limpar a pasta de confirmação no S3: " + e.getMessage());
        }
    }
}
