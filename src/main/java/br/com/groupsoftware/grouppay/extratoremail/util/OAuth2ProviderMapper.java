package br.com.groupsoftware.grouppay.extratoremail.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilitário para mapear entre identificadores de provedores de email
 * e provedores OAuth2.
 *
 * Contexto:
 * - tb_email_config.provider usa 'outlook', 'gmail', 'yahoo' (nomes de serviços de email)
 * - oauth2_provider usa 'microsoft', 'google', 'yahoo' (nomes de plataformas OAuth2)
 *
 * Esta classe faz a ponte entre os dois contextos.
 *
 * @author MailReader Development Team
 * @since 2026-01-07
 */
public class OAuth2ProviderMapper {

    /**
     * Mapeamento: Email Provider → OAuth2 Provider
     *
     * Exemplos:
     * - 'outlook' (serviço de email) → 'microsoft' (plataforma OAuth2)
     * - 'gmail' (serviço de email) → 'google' (plataforma OAuth2)
     */
    private static final Map<String, String> EMAIL_TO_OAUTH2 = new HashMap<>();

    /**
     * Mapeamento reverso: OAuth2 Provider → Email Provider
     */
    private static final Map<String, String> OAUTH2_TO_EMAIL = new HashMap<>();

    static {
        // Microsoft / Outlook
        EMAIL_TO_OAUTH2.put("outlook", "microsoft");
        OAUTH2_TO_EMAIL.put("microsoft", "outlook");

        // Google / Gmail
        EMAIL_TO_OAUTH2.put("gmail", "google");
        OAUTH2_TO_EMAIL.put("google", "gmail");

        // Yahoo (mesmo nome nos dois contextos)
        EMAIL_TO_OAUTH2.put("yahoo", "yahoo");
        OAUTH2_TO_EMAIL.put("yahoo", "yahoo");

        // Zoho
        EMAIL_TO_OAUTH2.put("zoho", "zoho");
        OAUTH2_TO_EMAIL.put("zoho", "zoho");

        // iCloud / Apple
        EMAIL_TO_OAUTH2.put("icloud", "apple");
        OAUTH2_TO_EMAIL.put("apple", "icloud");

        // AOL
        EMAIL_TO_OAUTH2.put("aol", "aol");
        OAUTH2_TO_EMAIL.put("aol", "aol");
    }

    /**
     * Converte identificador de email provider para OAuth2 provider.
     *
     * @param emailProvider Nome do provedor de email (ex: "outlook", "gmail")
     * @return Nome do provedor OAuth2 (ex: "microsoft", "google")
     * @throws IllegalArgumentException se o provedor não for suportado
     */
    public static String toOAuth2Provider(String emailProvider) {
        if (emailProvider == null || emailProvider.trim().isEmpty()) {
            throw new IllegalArgumentException("Email provider cannot be null or empty");
        }

        String normalized = emailProvider.toLowerCase().trim();
        String oauth2Provider = EMAIL_TO_OAUTH2.get(normalized);

        if (oauth2Provider == null) {
            throw new IllegalArgumentException(
                    "No OAuth2 provider mapping found for email provider: " + emailProvider);
        }

        return oauth2Provider;
    }

    /**
     * Converte identificador de OAuth2 provider para email provider.
     *
     * @param oauth2Provider Nome do provedor OAuth2 (ex: "microsoft", "google")
     * @return Nome do provedor de email (ex: "outlook", "gmail")
     * @throws IllegalArgumentException se o provedor não for suportado
     */
    public static String toEmailProvider(String oauth2Provider) {
        if (oauth2Provider == null || oauth2Provider.trim().isEmpty()) {
            throw new IllegalArgumentException("OAuth2 provider cannot be null or empty");
        }

        String normalized = oauth2Provider.toLowerCase().trim();
        String emailProvider = OAUTH2_TO_EMAIL.get(normalized);

        if (emailProvider == null) {
            throw new IllegalArgumentException(
                    "No email provider mapping found for OAuth2 provider: " + oauth2Provider);
        }

        return emailProvider;
    }

    /**
     * Verifica se um provedor de email tem suporte OAuth2.
     *
     * @param emailProvider Nome do provedor de email
     * @return true se o provedor suporta OAuth2, false caso contrário
     */
    public static boolean hasOAuth2Support(String emailProvider) {
        if (emailProvider == null || emailProvider.trim().isEmpty()) {
            return false;
        }
        return EMAIL_TO_OAUTH2.containsKey(emailProvider.toLowerCase().trim());
    }

    /**
     * Verifica se um provedor OAuth2 está mapeado.
     *
     * @param oauth2Provider Nome do provedor OAuth2
     * @return true se o provedor está mapeado, false caso contrário
     */
    public static boolean isOAuth2ProviderMapped(String oauth2Provider) {
        if (oauth2Provider == null || oauth2Provider.trim().isEmpty()) {
            return false;
        }
        return OAUTH2_TO_EMAIL.containsKey(oauth2Provider.toLowerCase().trim());
    }
}
