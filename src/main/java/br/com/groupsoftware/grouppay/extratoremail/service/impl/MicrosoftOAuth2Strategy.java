package br.com.groupsoftware.grouppay.extratoremail.service.impl;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.EmailSearchConfig;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.OAuth2TokenResponse;
import br.com.groupsoftware.grouppay.extratoremail.exception.MailReaderException;
import br.com.groupsoftware.grouppay.extratoremail.repository.EmailSearchConfigRepository;
import br.com.groupsoftware.grouppay.extratoremail.service.OAuth2Strategy;
import com.fasterxml.jackson.databind.JsonNode;
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

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Implementação da estratégia OAuth2 para Microsoft (Microsoft 365, Outlook, Hotmail, Live).
 *
 * Suporta:
 * - Contas pessoais Microsoft (@outlook.com, @hotmail.com, @live.com)
 * - Contas corporativas Microsoft 365 (domínios gerenciados)
 *
 * OAuth2 Flow: Authorization Code Flow (RFC 6749)
 * Endpoints: Microsoft Identity Platform (v2.0)
 * Scopes: IMAP.AccessAsUser.All offline_access
 *
 * @author MailReader Development Team
 * @since 2026-01-05
 */
@Slf4j
@Service("microsoftOAuth2Strategy")
@RequiredArgsConstructor
public class MicrosoftOAuth2Strategy implements OAuth2Strategy {

    private final RestTemplate restTemplate;
    private final EmailSearchConfigRepository emailSearchConfigRepository;

    @Value("${microsoft.oauth2.client-id}")
    private String clientId;

    @Value("${microsoft.oauth2.client-secret}")
    private String clientSecret;

    @Value("${microsoft.oauth2.redirect-uri}")
    private String redirectUri;

    private static final String PROVIDER_NAME = "microsoft";
    private static final String AUTHORIZATION_ENDPOINT = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";
    private static final String TOKEN_ENDPOINT = "https://login.microsoftonline.com/common/oauth2/v2.0/token";
    private static final String SCOPES = "https://outlook.office365.com/IMAP.AccessAsUser.All offline_access";

    /**
     * Domínios de email pessoais da Microsoft que são sempre suportados.
     */
    private static final Set<String> MICROSOFT_PERSONAL_DOMAINS = Set.of(
            "outlook.com",
            "hotmail.com",
            "live.com",
            "msn.com"
    );

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public String generateAuthorizationUrl(Long emailSearchConfigId) {
        log.info("Gerando URL de autorização OAuth2 Microsoft para EmailSearchConfig ID: {}", emailSearchConfigId);

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
        log.info("Trocando authorization code por tokens Microsoft para state: {}", state);

        try {
            Long emailSearchConfigId = Long.parseLong(state);
            EmailSearchConfig config = emailSearchConfigRepository.findById(emailSearchConfigId)
                    .orElseThrow(() -> new MailReaderException("Configuração de email não encontrada: " + emailSearchConfigId));

            OAuth2TokenResponse tokenResponse = requestTokens("authorization_code", code, null);

            saveTokens(config, tokenResponse);

            log.info("Tokens OAuth2 Microsoft salvos com sucesso para EmailSearchConfig ID: {}", emailSearchConfigId);

        } catch (NumberFormatException e) {
            log.error("State inválido recebido: {}", state);
            throw new MailReaderException("State inválido: " + state);
        } catch (RestClientException e) {
            log.error("Erro ao trocar authorization code por tokens Microsoft: {}", e.getMessage());
            throw new MailReaderException("Erro ao obter tokens da Microsoft: " + e.getMessage());
        }
    }

    @Override
    public String refreshAccessToken(EmailSearchConfig emailSearchConfig) {
        log.info("Renovando access token Microsoft para EmailSearchConfig ID: {}", emailSearchConfig.getId());

        if (emailSearchConfig.getOauth2RefreshToken() == null || emailSearchConfig.getOauth2RefreshToken().isBlank()) {
            log.error("Refresh token não encontrado para EmailSearchConfig ID: {}", emailSearchConfig.getId());
            throw new MailReaderException("Refresh token não disponível. É necessário autorizar novamente.");
        }

        try {
            OAuth2TokenResponse tokenResponse = requestTokens(
                    "refresh_token",
                    null,
                    emailSearchConfig.getOauth2RefreshToken()
            );

            saveTokens(emailSearchConfig, tokenResponse);

            log.info("Access token Microsoft renovado com sucesso para EmailSearchConfig ID: {}", emailSearchConfig.getId());

            return tokenResponse.getAccessToken();

        } catch (RestClientException e) {
            log.error("Erro ao renovar access token Microsoft: {}", e.getMessage());
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
                log.info("Token Microsoft expirado, renovando para EmailSearchConfig ID: {}", emailSearchConfig.getId());
                return refreshAccessToken(emailSearchConfig);
            }
            return emailSearchConfig.getOauth2AccessToken();
        }

        throw new MailReaderException("OAuth2 não está habilitado para esta configuração");
    }

    @Override
    public boolean supportsEmailDomain(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String normalizedEmail = email.toLowerCase().trim();

        // Extrai o domínio do email
        if (!normalizedEmail.contains("@")) {
            return false;
        }

        String domain = normalizedEmail.substring(normalizedEmail.indexOf("@") + 1);

        // Verifica se é um domínio pessoal Microsoft conhecido
        if (MICROSOFT_PERSONAL_DOMAINS.contains(domain)) {
            log.debug("Email '{}' pertence a domínio pessoal Microsoft", email);
            return true;
        }

        // Para outros domínios, verifica se é gerenciado pela Microsoft (Microsoft 365)
        // usando a API UserRealm da Microsoft
        return isManagedMicrosoftDomain(normalizedEmail);
    }

    /**
     * Verifica se um domínio de email é gerenciado pela Microsoft (Microsoft 365).
     *
     * Usa a API UserRealm da Microsoft para determinar se o domínio é "managed" (corporativo).
     * Domínios "federated" também são considerados Microsoft 365.
     *
     * @param email Email completo para verificação
     * @return true se o domínio é gerenciado pela Microsoft, false caso contrário
     */
    private boolean isManagedMicrosoftDomain(String email) {
        try {
            String userRealmUrl = "https://login.microsoftonline.com/common/UserRealm/" + email + "?api-version=1.0";

            JsonNode responseBody = restTemplate.getForObject(userRealmUrl, JsonNode.class);

            if (responseBody == null || !responseBody.has("account_type")) {
                log.debug("Email '{}' não retornou account_type da API UserRealm", email);
                return false;
            }

            String accountType = responseBody.get("account_type").asText().toLowerCase();

            // "managed" = Microsoft 365 (Azure AD)
            // "federated" = Federado com Microsoft (também Microsoft 365)
            // "unknown" = Não é Microsoft
            boolean isManaged = "managed".equals(accountType) || "federated".equals(accountType);

            log.debug("Email '{}' - account_type: {}, isManaged: {}", email, accountType, isManaged);

            return isManaged;

        } catch (Exception e) {
            // Em caso de erro na API (rede, timeout, etc.), assume que não é Microsoft
            log.warn("Erro ao verificar domínio Microsoft para email '{}': {}", email, e.getMessage());
            return false;
        }
    }

    /**
     * Faz a requisição de tokens para a Microsoft (authorization_code ou refresh_token).
     *
     * @param grantType Tipo de grant: "authorization_code" ou "refresh_token"
     * @param code Authorization code (usado apenas se grantType for "authorization_code")
     * @param refreshToken Refresh token (usado apenas se grantType for "refresh_token")
     * @return Response com os tokens
     */
    private OAuth2TokenResponse requestTokens(String grantType, String code, String refreshToken) {
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

        ResponseEntity<OAuth2TokenResponse> response = restTemplate.postForEntity(
                TOKEN_ENDPOINT,
                request,
                OAuth2TokenResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new MailReaderException("Falha ao obter tokens da Microsoft. Status: " + response.getStatusCode());
        }

        return response.getBody();
    }

    /**
     * Salva os tokens OAuth2 no banco de dados.
     *
     * @param config Configuração de email
     * @param tokenResponse Response da Microsoft com os tokens
     */
    private void saveTokens(EmailSearchConfig config, OAuth2TokenResponse tokenResponse) {
        config.setOauth2AccessToken(tokenResponse.getAccessToken());

        // Refresh token pode não vir em todas as respostas (ao renovar com refresh_token)
        if (tokenResponse.getRefreshToken() != null) {
            config.setOauth2RefreshToken(tokenResponse.getRefreshToken());
        }

        // Calcula a data de expiração baseado no expires_in (em segundos)
        LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn());
        config.setOauth2TokenExpiry(expiryTime);

        config.setOauth2Enabled(true);

        // Define o provedor como "microsoft"
        config.setOauth2Provider(PROVIDER_NAME);

        emailSearchConfigRepository.save(config);
    }
}
