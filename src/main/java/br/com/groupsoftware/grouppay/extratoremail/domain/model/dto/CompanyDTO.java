package br.com.groupsoftware.grouppay.extratoremail.domain.model.dto;

import lombok.*;

/**
 * DTO para representação de uma empresa (Company).
 * <p>
 * Esta classe é usada para transferir dados relacionados a uma empresa,
 * incluindo informações como email, UUID, código IBGE, senha e CNPJ.
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
public class CompanyDTO {
    private boolean isActive;
    private String email;
    private String uuid;
    private Long ibgeCode;
    private String password;
    private String cnpj;
    private String fantasyName;
    private String legalName;
}