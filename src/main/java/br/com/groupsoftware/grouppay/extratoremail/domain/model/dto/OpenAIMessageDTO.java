package br.com.groupsoftware.grouppay.extratoremail.domain.model.dto;

import lombok.*;

import java.io.Serializable;

/**
 * Classe de request para representar uma mensagem enviada em um diálogo com o OpenAI.
 * <p>
 * Contém os campos necessários para enviar uma mensagem, incluindo o papel (role) e o conteúdo (content).
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OpenAIMessageDTO implements Serializable {
    public String role;
    public String content;
}
