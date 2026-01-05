package br.com.groupsoftware.grouppay.extratoremail.service;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.EmailSearchConfig;

public interface MicrosoftOAuth2Service {

    /**
     * Gera a URL de autorização OAuth2 da Microsoft para o usuário conceder permissões.
     *
     * @param emailSearchConfigId ID da configuração de email
     * @return URL de autorização completa
     */
    String generateAuthorizationUrl(Long emailSearchConfigId);

    /**
     * Troca o código de autorização por tokens de acesso e refresh.
     *
     * @param code Código de autorização retornado pela Microsoft
     * @param state Estado para validação CSRF (deve conter o emailSearchConfigId)
     */
    void exchangeCodeForTokens(String code, String state);

    /**
     * Renova o access token usando o refresh token armazenado.
     *
     * @param emailSearchConfig Configuração de email com refresh token
     * @return Access token renovado
     */
    String refreshAccessToken(EmailSearchConfig emailSearchConfig);

    /**
     * Verifica se o token OAuth2 está expirado ou próximo da expiração.
     *
     * @param emailSearchConfig Configuração de email
     * @return true se o token precisa ser renovado
     */
    boolean isTokenExpired(EmailSearchConfig emailSearchConfig);

    /**
     * Obtém um access token válido, renovando se necessário.
     *
     * @param emailSearchConfig Configuração de email
     * @return Access token válido
     */
    String getValidAccessToken(EmailSearchConfig emailSearchConfig);
}
