package br.com.groupsoftware.grouppay.extratoremail.security;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.ClientGroup;
import br.com.groupsoftware.grouppay.extratoremail.repository.ClientGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Gerenciador de tokens JWT para o ClientGroup.
 * <p>
 * Esta classe é responsável por gerenciar a autenticação do ClientGroup
 * com o sistema externo, gerando e renovando tokens JWT quando necessário.
 * Ela utiliza credenciais derivadas do CNPJ e código de suporte para realizar
 * o login via Orquestrador.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupPayTokenManager {

    private final ClientGroupRepository clientGroupRepository;
    private final RestTemplate restTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${group-pay.auth}")
    private String host;

    public String getToken(ClientGroup clientGroup) {
        if (clientGroup.getToken() == null || jwtTokenProvider.isTokenExpired(clientGroup.getToken())) {
            return renewToken(clientGroup);
        }
        return clientGroup.getToken();
    }

    public String renewToken(ClientGroup clientGroup) {
        log.info("Renovando token para ClientGroup: {}", clientGroup.getUuid());

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    String.format("%s/api/autenticacao/login", host),
                    Map.of("username", clientGroup.getUsername(), "password", getPassword(clientGroup)),
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> body = response.getBody();
                if (body == null || !body.containsKey("payload")) {
                    log.error("Resposta inválida do servidor: Body está vazio ou sem payload");
                    throw new RuntimeException("Resposta inválida do servidor de autenticação");
                }

                // Extrai o payload e converte para o formato correto
                String payload = (String) body.get("payload");
                if (payload == null || payload.isEmpty()) {
                    log.error("Payload retornado está vazio");
                    throw new RuntimeException("Payload do servidor de autenticação está vazio");
                }

                clientGroup.setToken(payload);
                clientGroupRepository.save(clientGroup);
                log.info("Token renovado com sucesso para ClientGroup: {}", clientGroup.getUuid());
                return payload;
            } else {
                log.error("Falha ao renovar token. Status: {}", response.getStatusCode());
                throw new RuntimeException("Falha ao renovar token. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Erro ao renovar token para ClientGroup {}: {}", clientGroup.getUuid(), e.getMessage(), e);
            throw e;
        }
    }

    private String getPassword(ClientGroup clienteGroup) {
        return clienteGroup.getCnpj().concat("_").concat(clienteGroup.getCodigoSuporte());
    }
}