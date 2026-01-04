package br.com.groupsoftware.grouppay.extratoremail.domain.model.dto;

import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * Classe de request que representa uma requisição para a API da OpenAI.
 * <p>
 * Contém os campos necessários para enviar uma solicitação à API, incluindo o modelo e a lista de mensagens.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenAIDTO implements Serializable {
    public String model;
    public List<OpenAIMessageDTO> messages;
}

