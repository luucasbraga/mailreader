package br.com.groupsoftware.grouppay.extratoremail.controller;

import br.com.groupsoftware.grouppay.extratoremail.exception.MailReaderException;
import br.com.groupsoftware.grouppay.extratoremail.service.OAuth2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controlador REST unificado para gerenciamento do fluxo OAuth2 de múltiplos provedores.
 *
 * Suporta qualquer provedor OAuth2 registrado no sistema via Strategy Pattern:
 * - Microsoft (Outlook, Hotmail, Live, Microsoft 365)
 * - Google (Gmail, Google Workspace) - futuro
 * - Yahoo - futuro
 * - Outros provedores conforme necessário
 *
 * Endpoints:
 * - GET /api/v1/oauth2/{provider}/authorize/{emailSearchConfigId} - Inicia autorização
 * - GET /api/v1/oauth2/{provider}/callback - Recebe callback após autorização
 * - GET /api/v1/oauth2/{provider}/status/{emailSearchConfigId} - Verifica status
 *
 * Exemplos de uso:
 * - /api/v1/oauth2/microsoft/authorize/123
 * - /api/v1/oauth2/google/authorize/456
 * - /api/v1/oauth2/yahoo/callback?code=...&state=...
 *
 * @author MailReader Development Team
 * @since 2026-01-05
 */
@Slf4j
@Controller
@RequestMapping("/api/v1/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final OAuth2Service oauth2Service;

    /**
     * Inicia o fluxo de autorização OAuth2 para um provedor específico.
     * Redireciona o usuário para a página de login/consentimento do provedor.
     *
     * @param provider Nome do provedor OAuth2 (ex: "microsoft", "google", "yahoo")
     * @param emailSearchConfigId ID da configuração de email sendo autorizada
     * @return RedirectView para a URL de autorização do provedor
     */
    @GetMapping("/{provider}/authorize/{emailSearchConfigId}")
    public RedirectView authorize(
            @PathVariable String provider,
            @PathVariable Long emailSearchConfigId
    ) {
        log.info("Iniciando fluxo OAuth2 para provedor '{}' e EmailSearchConfig ID: {}",
                provider, emailSearchConfigId);

        try {
            String authorizationUrl = oauth2Service.generateAuthorizationUrl(emailSearchConfigId, provider);
            log.info("Redirecionando para {} OAuth2: {}", provider, authorizationUrl);
            return new RedirectView(authorizationUrl);

        } catch (IllegalArgumentException e) {
            log.error("Provedor OAuth2 inválido '{}': {}", provider, e.getMessage());
            return new RedirectView("/oauth2/error?message=Provedor+OAuth2+invalido:+" + provider);

        } catch (Exception e) {
            log.error("Erro ao gerar URL de autorização para provedor '{}': {}",
                    provider, e.getMessage(), e);
            return new RedirectView("/oauth2/error?message=Erro+ao+iniciar+autorizacao+OAuth2");
        }
    }

    /**
     * Callback do OAuth2 após autorização do usuário.
     * Recebe o authorization code e troca por access token e refresh token.
     *
     * Este endpoint é chamado pelo provedor OAuth2 após o usuário conceder permissões.
     *
     * @param provider Nome do provedor OAuth2
     * @param code Authorization code retornado pelo provedor
     * @param state State parameter para validação CSRF (contém emailSearchConfigId)
     * @param error Código de erro retornado pelo provedor (se houver)
     * @param errorDescription Descrição do erro (se houver)
     * @return RedirectView para página de sucesso ou erro
     */
    @GetMapping("/{provider}/callback")
    public RedirectView callback(
            @PathVariable String provider,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription
    ) {
        log.info("Callback OAuth2 recebido para provedor '{}' - state: {}, error: {}",
                provider, state, error);

        // Verifica se houve erro na autorização
        if (error != null) {
            log.error("Erro na autorização OAuth2 do provedor '{}': {} - {}",
                    provider, error, errorDescription);
            String errorMessage = errorDescription != null ? errorDescription : error;
            return new RedirectView("/oauth2/error?message=" + errorMessage + "&provider=" + provider);
        }

        // Valida parâmetros obrigatórios
        if (code == null || code.isBlank() || state == null || state.isBlank()) {
            log.error("Parâmetros inválidos no callback OAuth2 do provedor '{}' - code: {}, state: {}",
                    provider, code, state);
            return new RedirectView("/oauth2/error?message=Parametros+invalidos&provider=" + provider);
        }

        try {
            // Troca o authorization code por tokens usando a strategy correta
            oauth2Service.exchangeCodeForTokens(code, state, provider);

            log.info("Autorização OAuth2 concluída com sucesso para provedor '{}' e state: {}",
                    provider, state);
            return new RedirectView("/oauth2/success?emailSearchConfigId=" + state + "&provider=" + provider);

        } catch (IllegalArgumentException e) {
            log.error("Provedor OAuth2 inválido '{}' no callback: {}", provider, e.getMessage());
            return new RedirectView("/oauth2/error?message=Provedor+OAuth2+invalido&provider=" + provider);

        } catch (MailReaderException e) {
            log.error("Erro ao processar callback OAuth2 do provedor '{}': {}",
                    provider, e.getMessage(), e);
            return new RedirectView("/oauth2/error?message=" + e.getMessage() + "&provider=" + provider);

        } catch (Exception e) {
            log.error("Erro inesperado ao processar callback OAuth2 do provedor '{}': {}",
                    provider, e.getMessage(), e);
            return new RedirectView("/oauth2/error?message=Erro+ao+processar+autorizacao&provider=" + provider);
        }
    }

    /**
     * Endpoint REST para verificar o status da autorização OAuth2.
     *
     * @param provider Nome do provedor OAuth2
     * @param emailSearchConfigId ID da configuração de email
     * @return ResponseEntity com status da autorização
     */
    @GetMapping("/{provider}/status/{emailSearchConfigId}")
    @ResponseBody
    public ResponseEntity<OAuthStatusResponse> getOAuthStatus(
            @PathVariable String provider,
            @PathVariable Long emailSearchConfigId
    ) {
        log.debug("Verificando status OAuth2 para provedor '{}' e EmailSearchConfig ID: {}",
                provider, emailSearchConfigId);

        try {
            // Verifica se o provedor é válido
            if (!oauth2Service.getAvailableProviders().contains(provider.toLowerCase())) {
                return ResponseEntity.badRequest()
                        .body(new OAuthStatusResponse(
                                false,
                                "Provedor OAuth2 inválido: " + provider,
                                null
                        ));
            }

            // TODO: Implementar verificação de status completa
            // Por enquanto, retorna um placeholder
            return ResponseEntity.ok(new OAuthStatusResponse(
                    false,
                    "Status check not implemented",
                    provider
            ));

        } catch (Exception e) {
            log.error("Erro ao verificar status OAuth2: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new OAuthStatusResponse(
                            false,
                            "Erro ao verificar status: " + e.getMessage(),
                            provider
                    ));
        }
    }

    /**
     * Endpoint REST para listar provedores OAuth2 disponíveis.
     *
     * @return ResponseEntity com lista de provedores disponíveis
     */
    @GetMapping("/providers")
    @ResponseBody
    public ResponseEntity<ProvidersResponse> getAvailableProviders() {
        log.debug("Listando provedores OAuth2 disponíveis");

        try {
            var providers = oauth2Service.getAvailableProviders();
            return ResponseEntity.ok(new ProvidersResponse(providers));

        } catch (Exception e) {
            log.error("Erro ao listar provedores OAuth2: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * DTO para resposta de status OAuth2
     *
     * @param authorized Se a configuração está autorizada com OAuth2
     * @param message Mensagem descritiva do status
     * @param provider Nome do provedor OAuth2
     */
    public record OAuthStatusResponse(
            boolean authorized,
            String message,
            String provider
    ) {}

    /**
     * DTO para resposta de provedores disponíveis
     *
     * @param providers Lista de nomes de provedores OAuth2 disponíveis
     */
    public record ProvidersResponse(
            java.util.List<String> providers
    ) {}
}
