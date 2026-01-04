package br.com.groupsoftware.grouppay.extratoremail.domain.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * DTO para representar um arquivo obtido do Amazon S3.
 *
 * <p>Carrega os metadados essenciais do arquivo, incluindo nome, conteúdo em bytes
 * e o content-type correspondente. Permite transportar os dados necessários entre
 * camadas de serviço e controller para devolução ao cliente.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class S3ObjectDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String fileName;
    private byte[] content;
    private String contentType;
    private String recipientEmail;
    private String s3Key;
}

