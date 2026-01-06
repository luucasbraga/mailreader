package br.com.groupsoftware.grouppay.extratoremail.service;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.EmailSearchConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service facade para operações OAuth2 multi-provedor.
 *
 * Este serviço usa {@link OAuth2StrategyFactory} para delegar operações para a
 * estratégia OAuth2 correta baseada no provedor.
 *
 * Este é o serviço principal que deve ser injetado em outros componentes
 * (como EmailServiceImpl) para realizar operações OAuth2.
 *
 * Vantagens do Facade:
 * - Simplifica a interface para clientes
 * - Oculta a complexidade da seleção de strategy
 * - Permite adicionar lógica transversal (logging, validação, etc.)
 * - Facilita testes (mock único ao invés de múltiplas strategies)
 *
 * @author MailReader Development Team
 * @since 2026-01-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2Service {

    private final OAuth2StrategyFactory strategyFactory;

    /**
     * Gera URL de autorização OAuth2 para um provedor específico.
     *
     * @param emailSearchConfigId ID da configuração de email
     * @param providerName Nome do provedor (ex: "microsoft", "google")
     * @return URL completa para autorização OAuth2
     * @throws IllegalArgumentException se o provedor não for suportado
     */
    public String generateAuthorizationUrl(Long emailSearchConfigId, String providerName) {
        log.info("Generating OAuth2 authorization URL for provider '{}' and emailSearchConfigId '{}'",
                providerName, emailSearchConfigId);

        OAuth2Strategy strategy = strategyFactory.getStrategy(providerName);
        String authUrl = strategy.generateAuthorizationUrl(emailSearchConfigId);

        log.debug("Authorization URL generated successfully for provider '{}'", providerName);
        return authUrl;
    }

    /**
     * Troca authorization code por tokens OAuth2.
     *
     * @param code Authorization code recebido do provedor
     * @param state State parameter (contém emailSearchConfigId)
     * @param providerName Nome do provedor
     * @throws IllegalArgumentException se code, state ou providerName forem inválidos
     */
    public void exchangeCodeForTokens(String code, String state, String providerName) {
        log.info("Exchanging authorization code for tokens with provider '{}'", providerName);

        OAuth2Strategy strategy = strategyFactory.getStrategy(providerName);
        strategy.exchangeCodeForTokens(code, state);

        log.info("Tokens exchanged successfully with provider '{}'", providerName);
    }

    /**
     * Obtém um access token válido, renovando automaticamente se necessário.
     *
     * Este método detecta automaticamente o provedor baseado na configuração de email
     * e delega para a strategy correta.
     *
     * @param emailSearchConfig Configuração de email OAuth2
     * @return Access token válido
     * @throws IllegalArgumentException se o provedor não puder ser detectado
     * @throws RuntimeException se não for possível obter um token válido
     */
    public String getValidAccessToken(EmailSearchConfig emailSearchConfig) {
        String providerName = detectProvider(emailSearchConfig);

        log.debug("Getting valid access token for provider '{}' and email '{}'",
                providerName, emailSearchConfig.getEmail());

        OAuth2Strategy strategy = strategyFactory.getStrategy(providerName);
        return strategy.getValidAccessToken(emailSearchConfig);
    }

    /**
     * Renova o access token usando o refresh token.
     *
     * @param emailSearchConfig Configuração contendo o refresh token
     * @param providerName Nome do provedor
     * @return Novo access token válido
     */
    public String refreshAccessToken(EmailSearchConfig emailSearchConfig, String providerName) {
        log.info("Refreshing access token for provider '{}' and email '{}'",
                providerName, emailSearchConfig.getEmail());

        OAuth2Strategy strategy = strategyFactory.getStrategy(providerName);
        return strategy.refreshAccessToken(emailSearchConfig);
    }

    /**
     * Verifica se o access token está expirado.
     *
     * @param emailSearchConfig Configuração contendo o token
     * @param providerName Nome do provedor
     * @return true se o token está expirado
     */
    public boolean isTokenExpired(EmailSearchConfig emailSearchConfig, String providerName) {
        OAuth2Strategy strategy = strategyFactory.getStrategy(providerName);
        return strategy.isTokenExpired(emailSearchConfig);
    }

    /**
     * Verifica se um domínio de email é suportado por algum provedor OAuth2.
     *
     * @param email Endereço de email
     * @return true se algum provedor suporta o domínio
     */
    public boolean isEmailSupported(String email) {
        try {
            strategyFactory.getStrategyForEmail(email);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Detecta o provedor OAuth2 baseado na configuração de email.
     *
     * Ordem de detecção:
     * 1. Campo oauth2Provider na configuração (se já foi configurado)
     * 2. Domínio do email (detecção automática via strategy)
     *
     * @param emailSearchConfig Configuração de email
     * @return Nome do provedor detectado
     * @throws IllegalArgumentException se nenhum provedor puder ser detectado
     */
    private String detectProvider(EmailSearchConfig emailSearchConfig) {
        // Prioridade 1: Campo oauth2_provider já configurado
        if (emailSearchConfig.getOauth2Provider() != null
                && !emailSearchConfig.getOauth2Provider().trim().isEmpty()) {
            String configuredProvider = emailSearchConfig.getOauth2Provider().toLowerCase().trim();
            log.debug("Provider detected from configuration: {}", configuredProvider);
            return configuredProvider;
        }

        // Prioridade 2: Detectar baseado no domínio do email
        String email = emailSearchConfig.getEmail();
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot detect OAuth2 provider: email is null or empty in EmailSearchConfig");
        }

        OAuth2Strategy strategy = strategyFactory.getStrategyForEmail(email);
        String detectedProvider = strategy.getProviderName();

        log.debug("Provider detected from email domain '{}': {}", email, detectedProvider);
        return detectedProvider;
    }

    /**
     * Retorna a lista de provedores OAuth2 disponíveis.
     *
     * @return Lista de nomes de provedores
     */
    public java.util.List<String> getAvailableProviders() {
        return strategyFactory.getAvailableProviders();
    }
}
