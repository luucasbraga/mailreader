package br.com.groupsoftware.grouppay.extratoremail.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory para obter a estratégia OAuth2 correta baseada no provedor ou email.
 *
 * Esta factory automaticamente injeta todas as implementações de {@link OAuth2Strategy}
 * disponíveis no contexto Spring e permite recuperá-las por nome ou domínio de email.
 *
 * Padrão Factory facilita adicionar novos provedores sem modificar código existente.
 * Novas strategies são automaticamente detectadas pelo Spring via @Service.
 *
 * @author MailReader Development Team
 * @since 2026-01-05
 */
@Slf4j
@Service
public class OAuth2StrategyFactory {

    private final Map<String, OAuth2Strategy> strategies;

    /**
     * Construtor que injeta todas as implementações de OAuth2Strategy disponíveis.
     *
     * Spring automaticamente detecta todos os beans que implementam OAuth2Strategy
     * e os injeta como uma List. A factory então cria um Map indexado por nome do provedor.
     *
     * @param strategyList Lista de todas as strategies OAuth2 disponíveis no contexto Spring
     */
    @Autowired
    public OAuth2StrategyFactory(List<OAuth2Strategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        OAuth2Strategy::getProviderName,
                        Function.identity()
                ));

        log.info("OAuth2StrategyFactory initialized with {} provider(s): {}",
                strategies.size(),
                String.join(", ", strategies.keySet()));
    }

    /**
     * Retorna a estratégia OAuth2 baseada no nome do provedor.
     *
     * @param providerName Nome do provedor em minúsculas (ex: "microsoft", "google", "yahoo")
     * @return Strategy OAuth2 correspondente ao provedor
     * @throws IllegalArgumentException se o provedor não for suportado
     */
    public OAuth2Strategy getStrategy(String providerName) {
        if (providerName == null || providerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider name cannot be null or empty");
        }

        String normalizedProviderName = providerName.toLowerCase().trim();
        OAuth2Strategy strategy = strategies.get(normalizedProviderName);

        if (strategy == null) {
            String availableProviders = String.join(", ", strategies.keySet());
            throw new IllegalArgumentException(
                    String.format("OAuth2 provider '%s' not supported. Available providers: %s",
                            normalizedProviderName, availableProviders)
            );
        }

        log.debug("Selected OAuth2 strategy: {}", normalizedProviderName);
        return strategy;
    }

    /**
     * Detecta e retorna a estratégia OAuth2 baseada no domínio do email.
     *
     * Itera sobre todas as strategies disponíveis e retorna a primeira que
     * suportar o domínio do email fornecido.
     *
     * Útil para detecção automática do provedor quando o usuário fornece apenas o email.
     *
     * @param email Endereço de email completo (ex: "usuario@outlook.com")
     * @return Strategy OAuth2 que suporta o domínio do email
     * @throws IllegalArgumentException se nenhum provedor suportar o domínio do email
     */
    public OAuth2Strategy getStrategyForEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        String normalizedEmail = email.toLowerCase().trim();

        return strategies.values().stream()
                .filter(strategy -> strategy.supportsEmailDomain(normalizedEmail))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("No OAuth2 provider found for email domain: %s. " +
                                        "Supported providers: %s",
                                normalizedEmail,
                                String.join(", ", strategies.keySet()))
                ));
    }

    /**
     * Verifica se um provedor OAuth2 está disponível.
     *
     * @param providerName Nome do provedor
     * @return true se o provedor está disponível, false caso contrário
     */
    public boolean hasProvider(String providerName) {
        if (providerName == null || providerName.trim().isEmpty()) {
            return false;
        }
        return strategies.containsKey(providerName.toLowerCase().trim());
    }

    /**
     * Retorna a lista de nomes de provedores OAuth2 disponíveis.
     *
     * @return Lista de nomes de provedores
     */
    public List<String> getAvailableProviders() {
        return List.copyOf(strategies.keySet());
    }
}
