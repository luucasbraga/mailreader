package br.com.groupsoftware.grouppay.extratoremail.service;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.EmailSearchConfig;

/**
 * Interface base para estratégias de OAuth2.
 * Cada provedor (Microsoft, Google, Yahoo, etc.) implementará esta interface.
 *
 * Padrão Strategy permite adicionar novos provedores OAuth2 sem modificar código existente.
 *
 * @author MailReader Development Team
 * @since 2026-01-05
 */
public interface OAuth2Strategy {

    /**
     * Retorna o identificador único do provedor OAuth2.
     *
     * @return Nome do provedor em minúsculas (ex: "microsoft", "google", "yahoo")
     */
    String getProviderName();

    /**
     * Gera a URL de autorização OAuth2 para redirecionar o usuário.
     *
     * O usuário será redirecionado para a página de login/consentimento do provedor.
     *
     * @param emailSearchConfigId ID da configuração de email sendo autorizada
     * @return URL completa para autorização OAuth2
     */
    String generateAuthorizationUrl(Long emailSearchConfigId);

    /**
     * Troca o authorization code por access token e refresh token.
     *
     * Chamado após o usuário conceder permissões e ser redirecionado de volta.
     * Os tokens são armazenados no EmailSearchConfig correspondente.
     *
     * @param code Authorization code recebido do provedor
     * @param state State parameter para validação CSRF (contém emailSearchConfigId)
     * @throws IllegalArgumentException se code ou state forem inválidos
     * @throws RuntimeException se a troca de tokens falhar
     */
    void exchangeCodeForTokens(String code, String state);

    /**
     * Renova o access token usando o refresh token.
     *
     * Quando o access token expira, usa o refresh token para obter um novo access token.
     * Atualiza os tokens no EmailSearchConfig.
     *
     * @param emailSearchConfig Configuração contendo o refresh token
     * @return Novo access token válido
     * @throws RuntimeException se o refresh token estiver inválido ou expirado
     */
    String refreshAccessToken(EmailSearchConfig emailSearchConfig);

    /**
     * Verifica se o access token está expirado ou próximo da expiração.
     *
     * Recomenda-se usar um buffer (ex: 5 minutos) para renovar antes da expiração real.
     *
     * @param emailSearchConfig Configuração contendo o token e data de expiração
     * @return true se o token está expirado ou próximo da expiração, false caso contrário
     */
    boolean isTokenExpired(EmailSearchConfig emailSearchConfig);

    /**
     * Obtém um access token válido, renovando-o automaticamente se necessário.
     *
     * Este é o método principal usado pelos serviços de email.
     * Verifica a expiração e renova o token automaticamente se necessário.
     *
     * @param emailSearchConfig Configuração de email OAuth2
     * @return Access token válido e não expirado
     * @throws RuntimeException se não for possível obter um token válido
     */
    String getValidAccessToken(EmailSearchConfig emailSearchConfig);

    /**
     * Verifica se este provedor OAuth2 suporta o domínio do email fornecido.
     *
     * Usado para detectar automaticamente qual provedor usar baseado no email do usuário.
     *
     * Exemplos:
     * - Microsoft: outlook.com, hotmail.com, live.com, domínios corporativos Microsoft 365
     * - Google: gmail.com, googlemail.com, domínios Google Workspace
     * - Yahoo: yahoo.com, yahoo.com.br, etc.
     *
     * @param email Endereço de email completo (ex: "usuario@outlook.com")
     * @return true se este provedor suporta o domínio do email, false caso contrário
     */
    boolean supportsEmailDomain(String email);
}
