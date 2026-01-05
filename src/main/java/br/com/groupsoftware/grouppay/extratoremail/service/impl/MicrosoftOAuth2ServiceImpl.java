package br.com.groupsoftware.grouppay.extratoremail.service.impl;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.EmailSearchConfig;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.MicrosoftOAuth2TokenResponse;
import br.com.groupsoftware.grouppay.extratoremail.exception.MailReaderException;
import br.com.groupsoftware.grouppay.extratoremail.repository.EmailSearchConfigRepository;
import br.com.groupsoftware.grouppay.extratoremail.service.MicrosoftOAuth2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MicrosoftOAuth2ServiceImpl implements MicrosoftOAuth2Service {

    private final RestTemplate restTemplate;
    private final EmailSearchConfigRepository emailSearchConfigRepository;

    @Value("${microsoft.oauth2.client-id}")
    private String clientId;

    @Value("${microsoft.oauth2.client-secret}")
    private String clientSecret;

    @Value("${microsoft.oauth2.redirect-uri}")
    private String redirectUri;

    private static final String AUTHORIZATION_ENDPOINT = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";
    private static final String TOKEN_ENDPOINT = "https://login.microsoftonline.com/common/oauth2/v2.0/token";
    private static final String SCOPES = "https://outlook.office365.com/IMAP.AccessAsUser.All offline_access";

    @Override
    public String generateAuthorizationUrl(Long emailSearchConfigId) {
        log.info("Gerando URL de autorização OAuth2 para EmailSearchConfig ID: {}", emailSearchConfigId);

        String state = String.valueOf(emailSearchConfigId);

        return UriComponentsBuilder.fromHttpUrl(AUTHORIZATION_ENDPOINT)
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_mode", "query")
                .queryParam("scope", SCOPES)
                .queryParam("state", state)
                .toUriString();
    }

    @Override
    public void exchangeCodeForTokens(String code, String state) {
        log.info("Trocando authorization code por tokens para state: {}", state);

        try {
            Long emailSearchConfigId = Long.parseLong(state);
            EmailSearchConfig config = emailSearchConfigRepository.findById(emailSearchConfigId)
                    .orElseThrow(() -> new MailReaderException("Configuração de email não encontrada: " + emailSearchConfigId));

            MicrosoftOAuth2TokenResponse tokenResponse = requestTokens("authorization_code", code, null);

            saveTokens(config, tokenResponse);

            log.info("Tokens OAuth2 salvos com sucesso para EmailSearchConfig ID: {}", emailSearchConfigId);

        } catch (NumberFormatException e) {
            log.error("State inválido recebido: {}", state);
            throw new MailReaderException("State inválido: " + state);
        } catch (RestClientException e) {
            log.error("Erro ao trocar authorization code por tokens: {}", e.getMessage());
            throw new MailReaderException("Erro ao obter tokens da Microsoft: " + e.getMessage());
        }
    }

    @Override
    public String refreshAccessToken(EmailSearchConfig emailSearchConfig) {
        log.info("Renovando access token para EmailSearchConfig ID: {}", emailSearchConfig.getId());

        if (emailSearchConfig.getOauth2RefreshToken() == null || emailSearchConfig.getOauth2RefreshToken().isBlank()) {
            log.error("Refresh token não encontrado para EmailSearchConfig ID: {}", emailSearchConfig.getId());
            throw new MailReaderException("Refresh token não disponível. É necessário autorizar novamente.");
        }

        try {
            MicrosoftOAuth2TokenResponse tokenResponse = requestTokens(
                    "refresh_token",
                    null,
                    emailSearchConfig.getOauth2RefreshToken()
            );

            saveTokens(emailSearchConfig, tokenResponse);

            log.info("Access token renovado com sucesso para EmailSearchConfig ID: {}", emailSearchConfig.getId());

            return tokenResponse.getAccessToken();

        } catch (RestClientException e) {
            log.error("Erro ao renovar access token: {}", e.getMessage());
            throw new MailReaderException("Erro ao renovar token da Microsoft: " + e.getMessage());
        }
    }

    @Override
    public boolean isTokenExpired(EmailSearchConfig emailSearchConfig) {
        if (emailSearchConfig.getOauth2TokenExpiry() == null) {
            return true;
        }

        // Considera expirado se falta menos de 5 minutos para expirar
        LocalDateTime expiryThreshold = LocalDateTime.now().plusMinutes(5);
        return emailSearchConfig.getOauth2TokenExpiry().isBefore(expiryThreshold);
    }

    @Override
    public String getValidAccessToken(EmailSearchConfig emailSearchConfig) {
        if (emailSearchConfig.getOauth2Enabled() != null && emailSearchConfig.getOauth2Enabled()) {
            if (isTokenExpired(emailSearchConfig)) {
                log.info("Token expirado, renovando para EmailSearchConfig ID: {}", emailSearchConfig.getId());
                return refreshAccessToken(emailSearchConfig);
            }
            return emailSearchConfig.getOauth2AccessToken();
        }

        throw new MailReaderException("OAuth2 não está habilitado para esta configuração");
    }

    /**
     * Faz a requisição de tokens para a Microsoft (tanto para authorization_code quanto para refresh_token).
     *
     * @param grantType Tipo de grant: "authorization_code" ou "refresh_token"
     * @param code Authorization code (usado apenas se grantType for "authorization_code")
     * @param refreshToken Refresh token (usado apenas se grantType for "refresh_token")
     * @return Response com os tokens
     */
    private MicrosoftOAuth2TokenResponse requestTokens(String grantType, String code, String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("grant_type", grantType);
        formData.add("redirect_uri", redirectUri);

        if ("authorization_code".equals(grantType)) {
            formData.add("code", code);
        } else if ("refresh_token".equals(grantType)) {
            formData.add("refresh_token", refreshToken);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        ResponseEntity<MicrosoftOAuth2TokenResponse> response = restTemplate.postForEntity(
                TOKEN_ENDPOINT,
                request,
                MicrosoftOAuth2TokenResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new MailReaderException("Falha ao obter tokens da Microsoft. Status: " + response.getStatusCode());
        }

        return response.getBody();
    }

    /**
     * Salva os tokens no banco de dados.
     *
     * @param config Configuração de email
     * @param tokenResponse Response da Microsoft com os tokens
     */
    private void saveTokens(EmailSearchConfig config, MicrosoftOAuth2TokenResponse tokenResponse) {
        config.setOauth2AccessToken(tokenResponse.getAccessToken());

        // Refresh token pode não vir em todas as respostas (por exemplo, ao renovar com refresh_token)
        if (tokenResponse.getRefreshToken() != null) {
            config.setOauth2RefreshToken(tokenResponse.getRefreshToken());
        }

        // Calcula a data de expiração baseado no expires_in (em segundos)
        LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn());
        config.setOauth2TokenExpiry(expiryTime);

        config.setOauth2Enabled(true);

        emailSearchConfigRepository.save(config);
    }
}
