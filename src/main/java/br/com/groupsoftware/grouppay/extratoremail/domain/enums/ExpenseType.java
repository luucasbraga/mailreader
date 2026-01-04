package br.com.groupsoftware.grouppay.extratoremail.domain.enums;

/**
 * Enumeração que representa os tipos de PDF suportados pelo sistema.
 * <p>
 * Define os diferentes tipos de documentos em formato PDF que podem ser processados,
 * como notas fiscais eletrônicas, boletos bancários e outros.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */

public enum ExpenseType {
    NFE,   // Nota Fiscal Eletrônica
    NFSE,  // Nota Fiscal de Serviços Eletrônica
    NFCE,  // Nota Fiscal do Consumidor Eletrônica
    NF3E,  // Nota Fiscal de Energia Elétrica Eletrônica
    CTE,   // Conhecimento de Transporte Eletrônico
    BOLETO, // Boleto Bancário
    FATURA, // Faturas comuns
    DARF,  // Documento de Arrecadação de Receitas Federais
    FGTS,  // Guia do FGTS Digital
    GPS,   // Guia da Previdência Social
    OUTRO   // Documento desconhecido ou não identificado
}

