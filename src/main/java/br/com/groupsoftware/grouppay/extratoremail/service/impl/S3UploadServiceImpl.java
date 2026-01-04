package br.com.groupsoftware.grouppay.extratoremail.service.impl;

import br.com.groupsoftware.grouppay.extratoremail.service.S3UploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Implementação do serviço de integração com o Amazon S3.
 *
 * <p>Esta classe fornece a funcionalidade para fazer upload de arquivos locais para
 * um bucket S3 específico. O método principal, {@link #uploadFile(Path, String)}, realiza
 * o envio de um arquivo para a chave especificada no bucket S3 e retorna a eTag como
 * confirmação de sucesso.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Slf4j
@Service
class S3UploadServiceImpl implements S3UploadService {

    private final S3Client s3Client;
    private final String bucketName;

    public S3UploadServiceImpl(
            @Value("${aws.s3-upload.bucket-name}") String bucketName,
            @Value("${aws.s3-upload.access-key-id}") String accessKeyId,
            @Value("${aws.s3-upload.secret-access-key}") String secretAccessKey,
            @Value("${aws.s3-upload.region}") String region) {

        // Configura o cliente S3
        this.s3Client = S3Client.builder()
                .region(Region.of(region)) // Configura a região (sa-east-1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofMinutes(2))
                        .apiCallAttemptTimeout(Duration.ofSeconds(30))
                        .build())
                .build();
        this.bucketName = bucketName;
    }

    @Override
    public String uploadFile(Path filePath, String url) throws S3Exception {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("A URL de destino não pode ser nula ou vazia.");
        }

        String s3Key = url;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        try {
            // Envio do arquivo para o S3
            PutObjectResponse response = s3Client.putObject(request, RequestBody.fromFile(filePath));
            log.info("Arquivo {} enviado com sucesso para {}. ETag: {}", filePath.getFileName(), s3Key, response.eTag());
            return response.eTag();
        } catch (S3Exception e) {
            log.error("Erro ao enviar arquivo {} para o S3 em {}. Código de erro: {}, Mensagem: {}",
                    filePath.getFileName(), s3Key, e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage());
            throw e;
        }
    }
}
