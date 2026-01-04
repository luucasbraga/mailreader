package br.com.groupsoftware.grouppay.extratoremail.controller;

import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.LoginDTO;
import br.com.groupsoftware.grouppay.extratoremail.security.JwtTokenProvider;
import br.com.groupsoftware.grouppay.extratoremail.service.ServiceFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador responsável por autenticação e registro de usuários.
 *
 * <p>Gerencia endpoints para login e registro de novos usuários, utilizando um
 * {@link JwtTokenProvider} para gerar tokens de autenticação e delegando
 * as operações de autenticação e registro ao {@link ServiceFacade}.</p>
 *
 * <p>Os tokens gerados são utilizados para autenticar as requisições subsequentes no sistema.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final ServiceFacade service;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginDTO loginDTO) throws Exception {
        if (service.user.authenticate(loginDTO.username(), loginDTO.password())) {
            String token = jwtTokenProvider.generateToken(loginDTO.username());
            return ResponseEntity.ok(token);
        }
        return ResponseEntity.status(401).body("Usuário ou senha inválidos");
    }

    @PostMapping("/register")
    public String register(@RequestBody LoginDTO loginDTO) throws Exception {
        try {
            service.user.registerUser(loginDTO.username(), loginDTO.password());
            return "User registered successfully";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}

