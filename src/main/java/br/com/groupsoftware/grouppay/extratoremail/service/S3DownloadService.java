package br.com.groupsoftware.grouppay.extratoremail.service;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Company;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.S3ObjectDTO;
import br.com.groupsoftware.grouppay.extratoremail.exception.MailReaderException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Interface para o serviço de download de arquivos do Amazon S3.
 *
 * <p>Define o método necessário para realizar o download de arquivos armazenados
 * no Amazon S3 para um diretório local, com base nas configurações de uma empresa
 * específica.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
public interface S3DownloadService {
    void downloadFiles(Company company) throws S3Exception;

    void createBucketAndSesAliasForCompany(String email);

    S3ObjectDTO downloadConfirmationFile(String codigoSuporte) throws MailReaderException;

    void deleteConfirmationFile(String s3Key) throws MailReaderException;

    void deleteConfirmationFolder(String codigoSuporte) throws MailReaderException;
}
