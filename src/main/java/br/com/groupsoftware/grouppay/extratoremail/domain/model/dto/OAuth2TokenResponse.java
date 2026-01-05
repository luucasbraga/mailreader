package br.com.groupsoftware.grouppay.extratoremail.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO genérico para respostas de token OAuth2.
 *
 * Este DTO é compatível com a maioria dos provedores OAuth2 (Microsoft, Google, Yahoo, etc.)
 * que seguem o padrão RFC 6749 (OAuth 2.0).
 *
 * Campos comuns entre provedores:
 * - access_token: Token de acesso para autenticação
 * - token_type: Tipo do token (geralmente "Bearer")
 * - expires_in: Tempo de vida do token em segundos
 * - refresh_token: Token para renovação do access_token
 * - scope: Escopos/permissões concedidas
 *
 * @author MailReader Development Team
 * @since 2026-01-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2TokenResponse {

    /**
     * Access token usado para autenticar requisições à API do provedor.
     * Geralmente tem tempo de vida curto (ex: 1 hora).
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * Tipo do token (geralmente "Bearer").
     * Define como o token deve ser usado na autorização HTTP.
     */
    @JsonProperty("token_type")
    private String tokenType;

    /**
     * Tempo de vida do access token em segundos.
     * Após este período, o token expira e deve ser renovado.
     */
    @JsonProperty("expires_in")
    private Integer expiresIn;

    /**
     * Escopos/permissões concedidas pelo usuário.
     * Lista separada por espaço das permissões autorizadas.
     */
    @JsonProperty("scope")
    private String scope;

    /**
     * Refresh token usado para obter novos access tokens.
     * Geralmente tem tempo de vida longo ou ilimitado.
     */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * ID token (usado por OpenID Connect).
     * Contém informações de identidade do usuário em formato JWT.
     * Opcional - nem todos os provedores retornam este campo.
     */
    @JsonProperty("id_token")
    private String idToken;

    /**
     * Informações adicionais específicas do provedor.
     * Campo flexível para extensões não-padrão.
     */
    @JsonProperty("ext_expires_in")
    private Integer extExpiresIn;
}
