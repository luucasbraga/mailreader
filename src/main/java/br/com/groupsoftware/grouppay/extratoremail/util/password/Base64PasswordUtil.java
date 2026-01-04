package br.com.groupsoftware.grouppay.extratoremail.util.password;

import lombok.experimental.UtilityClass;

import java.util.Base64;

/**
 * Utilitário para codificação e decodificação de strings utilizando Base64.
 *
 * <p>Esta classe fornece métodos utilitários para verificar se uma string
 * está codificada em Base64, bem como codificar e decodificar strings
 * utilizando o padrão Base64.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@UtilityClass
public class Base64PasswordUtil {

    public static boolean isStringCodificadaEmBase64(String string) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(string);
            return Base64.getEncoder().encodeToString(decodedBytes).equals(string);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String encode(String string) {
        return Base64.getEncoder().encodeToString(string.getBytes());
    }

    public static String decode(String string) {
        if (string == null || string.isBlank()) {
            throw new IllegalArgumentException("String não pode ser nula ou vazia para decodificação Base64");
        }
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(string);
            return new String(decodedBytes);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Erro ao decodificar string Base64. A string pode não estar codificada corretamente: " + e.getMessage(), e);
        }
    }

}

