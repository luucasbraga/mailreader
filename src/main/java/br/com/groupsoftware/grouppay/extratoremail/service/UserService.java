package br.com.groupsoftware.grouppay.extratoremail.service;

/**
 * Serviço responsável pelo gerenciamento de usuários.
 *
 * <p>Define operações para registro e autenticação de usuários.
 * Implementações desta interface devem garantir que as credenciais
 * dos usuários sejam armazenadas e verificadas de forma segura.</p>
 *
 * <p>O registro inclui a criação de credenciais seguras utilizando técnicas
 * como hashing e salting. A autenticação verifica se as credenciais fornecidas
 * correspondem às credenciais armazenadas.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
public interface UserService {
    void registerUser(String username, String password) throws Exception;

    boolean authenticate(String username, String password) throws Exception;
}
