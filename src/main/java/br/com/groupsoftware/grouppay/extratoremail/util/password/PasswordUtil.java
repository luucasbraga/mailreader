package br.com.groupsoftware.grouppay.extratoremail.util.password;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utilitário para manipulação segura de senhas.
 *
 * <p>Fornece métodos para gerar sal aleatório, realizar hashing de senhas
 * com sal utilizando o algoritmo SHA-256, e verificar a correspondência
 * entre uma senha fornecida e seu hash armazenado.</p>
 *
 * <p>Todos os métodos desta classe são estáticos, permitindo fácil integração
 * com outros componentes do sistema.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
public class PasswordUtil {

    /**
     * Gera um salt aleatório para ser usado no hashing de senha.
     *
     * @return O salt gerado como uma string Base64.
     */
    public static String generateSalt() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Gera o hash de uma senha utilizando SHA-256 combinado com um salt.
     *
     * @param password A senha em texto puro.
     * @param salt     O salt a ser utilizado no hash.
     * @return O hash gerado como uma string Base64.
     * @throws NoSuchAlgorithmException Caso o algoritmo SHA-256 não seja encontrado.
     */
    public static String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(salt.getBytes());
        byte[] hashedBytes = messageDigest.digest(password.getBytes());
        return Base64.getEncoder().encodeToString(hashedBytes);
    }

    /**
     * Verifica se a senha fornecida é igual à senha armazenada após hashing.
     *
     * @param enteredPassword A senha fornecida pelo usuário.
     * @param storedHash      O hash armazenado da senha.
     * @param salt            O salt usado no hash armazenado.
     * @return true se a senha for válida; false caso contrário.
     * @throws NoSuchAlgorithmException Caso o algoritmo SHA-256 não seja encontrado.
     */
    public static boolean verifyPassword(String enteredPassword, String storedHash, String salt) throws NoSuchAlgorithmException {
        String hashedPassword = hashPassword(enteredPassword, salt);
        return hashedPassword.equals(storedHash);
    }
}

