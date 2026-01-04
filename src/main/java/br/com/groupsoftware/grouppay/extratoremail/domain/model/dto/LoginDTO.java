package br.com.groupsoftware.grouppay.extratoremail.domain.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * Representa o payload da requisição de login.
 * <p>
 * Este DTO encapsula as credenciais necessárias para autenticação no sistema.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Builder
public record LoginDTO(
        @NotBlank(message = "O nome de usuário não pode estar vazio ou em branco.")
        String username,

        @NotBlank(message = "A senha não pode estar vazia ou em branco.")
        String password
) {
}



