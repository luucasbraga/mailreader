package br.com.groupsoftware.grouppay.extratoremail.domain.model.dto;

import lombok.*;

import java.io.Serializable;

/**
 * DTO para representação de documentos.
 * <p>
 * Esta classe é utilizada para transferir dados de documentos, incluindo informações
 * como o identificador do documento e o caminho de armazenamento na Amazon S3.
 * </p>
 *
 * <p>
 * Anotações do Lombok são usadas para gerar automaticamente getters, setters, construtores,
 * métodos toString, hashCode e equals, bem como o padrão Builder para facilitar a criação de objetos.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class DocumentDTO implements Serializable {
    private Long documentId;
    private String amazonPath;
}

