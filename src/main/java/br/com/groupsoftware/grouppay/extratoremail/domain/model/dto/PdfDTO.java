package br.com.groupsoftware.grouppay.extratoremail.domain.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) que representa os dados necessários para
 * processar arquivos PDF.
 * <p>
 * Contém informações como o caminho do arquivo e o nome ou sufixo do arquivo,
 * que são usados para identificar e processar arquivos PDF.
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
public class PdfDTO implements Serializable {
    private String filePath;
    private String fileNameOrSuffix;

    public String getFilePathName() {
        return filePath + fileNameOrSuffix;
    }
}