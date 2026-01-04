package br.com.groupsoftware.grouppay.extratoremail.domain.model.dto;

import lombok.*;

/**
 * DTO para representação de um grupo de clientes (ClientGroup).
 * <p>
 * Esta classe é usada para transferir dados relacionados a um grupo de clientes,
 * incluindo informações como UUID, nome de usuário, CNPJ, código de suporte
 * e a associação com uma empresa representada por um objeto {@link CompanyDTO}.
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
public class ClientGroupDTO {
    private String uuid;
    private String username;
    private String cnpj;
    private String codigoSuporte;
    private CompanyDTO company;
    private EmailSearchConfigDTO emailSearchConfig;
}