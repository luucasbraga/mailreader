package br.com.groupsoftware.grouppay.extratoremail.domain.model.dto;

import br.com.groupsoftware.grouppay.extratoremail.domain.enums.ExpenseType;
import lombok.*;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) para representação de despesas.
 * <p>
 * Este DTO encapsula os dados necessários para a transferência de informações
 * sobre despesas entre sistemas. Inclui identificador único, tipo da despesa e
 * uma representação em JSON dos detalhes adicionais.
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
public class ExpenseDTO implements Serializable {
    private Long documentId;
    private String fileName;
    private String codSupport;
    private ExpenseType type;
    private String json;
    private String amazonPath;
}