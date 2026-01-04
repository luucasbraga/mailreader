package br.com.groupsoftware.grouppay.extratoremail.domain.enums;

/**
 * Enumeração que define os possíveis estagios de um documento durante o seu processamento.
 * <p>
 * Cada valor representa uma etapa do ciclo de vida do documento, desde o download até o processamento final.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
public enum DocumentStage {
    DOWNLOADED,               // Documento foi baixado
    PASSWORD_REMOVED,         // Senha foi removida
    DELETED_FROM_DOWNLOAD,    // Documento foi deletado da pasta de download
    TEXT_EXTRACTED,           // Texto foi extraída do documento
    EXPENSE_EXTRACTED,        // Despesa foi extraída do documento
    SENT_TO_GROUP_PAY,        // Despesa enviada para o GroupPay
    SENT_TO_S3,               // Enviar documento para o S3
    DELETE_FROM_LOCAL,        // Documento deletado após processamento
    PROCESSED,                 // Processamento finalizado
    COMPANY_MATCHED,
    COMPANY_NOT_FOUND,
    ERRO
}