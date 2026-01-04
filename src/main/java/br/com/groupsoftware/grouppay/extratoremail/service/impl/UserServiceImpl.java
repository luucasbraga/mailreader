package br.com.groupsoftware.grouppay.extratoremail.service.impl;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.User;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import br.com.groupsoftware.grouppay.extratoremail.service.UserService;
import br.com.groupsoftware.grouppay.extratoremail.util.password.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Implementação do serviço de usuários.
 *
 * <p>Fornece métodos para registrar novos usuários e autenticar usuários existentes.
 * Utiliza {@link PasswordUtil} para geração de salt e hash de senhas, garantindo
 * a segurança das credenciais armazenadas.</p>
 *
 * <p>Os dados dos usuários são manipulados através do {@link RepositoryFacade}.</p>
 *
 * @author Marco
 * @version 1.0
 * @since 2024
 */
@Service
@RequiredArgsConstructor
class UserServiceImpl implements UserService {

    private final RepositoryFacade repository;

    public void registerUser(String username, String password) throws Exception {
        String salt = PasswordUtil.generateSalt();
        String hashedPassword = PasswordUtil.hashPassword(password, salt);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(hashedPassword);
        user.setSalt(salt);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        repository.user.save(user);
    }

    public boolean authenticate(String username, String password) throws Exception {
        User user = repository.user.findByUsername(username);
        if (user == null) {
            return false;
        }

        String hashedPassword = PasswordUtil.hashPassword(password, user.getSalt());
        return hashedPassword.equals(user.getPasswordHash());
    }
}

