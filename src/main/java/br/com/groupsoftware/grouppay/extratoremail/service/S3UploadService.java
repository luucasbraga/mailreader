package br.com.groupsoftware.grouppay.extratoremail.service;

import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.file.Path;

/**
 * Interface para o serviço de integração com o Amazon S3.
 *
 * <p>
 * Esta interface define os métodos necessários para operações de interação com
 * o Amazon S3, como o upload de arquivos para um bucket específico.
 * O método retorna o eTag do objeto enviado, que pode ser usado para confirmar o sucesso
 * do upload ou validar a integridade do arquivo armazenado.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
public interface S3UploadService {
    String uploadFile(Path filePath, String url) throws S3Exception;
}
