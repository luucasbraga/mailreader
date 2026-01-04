package br.com.groupsoftware.grouppay.extratoremail.domain.enums;

/**
 * Enumeração que representa os possíveis status de atualização de uma empresa.
 * <p>
 * Essa enumeração é utilizada para indicar o estado de atualização de uma empresa no sistema,
 * permitindo rastrear se uma atualização já foi processada ou ainda está pendente.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */

public enum CompanyUpdateStatus {
    NOT_UPDATED,  // Indica que a empresa ainda não foi atualizada.
    UPDATED      // Indica que a empresa já foi atualizada com sucesso.
}