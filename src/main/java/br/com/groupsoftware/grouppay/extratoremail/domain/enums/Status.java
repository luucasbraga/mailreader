package br.com.groupsoftware.grouppay.extratoremail.domain.enums;

/**
 * Enumeração que define os possíveis status de processamento de um e-mail ou documento.
 * <p>
 * Representa os diferentes estados que um e-mail ou documento pode ter em relação ao seu processamento,
 * como não processado ou em processamento.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */

public enum Status {
    NOT_PROCESSING,  // O e-mail não está sendo processado no momento
    PROCESSING      // O e-mail está sendo processado
}