package br.com.groupsoftware.grouppay.extratoremail.controller;

import br.com.groupsoftware.grouppay.extratoremail.exception.MailReaderException;
import br.com.groupsoftware.grouppay.extratoremail.service.MicrosoftOAuth2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controlador REST para gerenciamento do fluxo OAuth2 da Microsoft.
 * <p>
 * Fornece endpoints para iniciar o fluxo de autorização e receber o callback
 * da Microsoft após o usuário conceder permissões.
 * </p>
 *
 * @author Claude
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Controller
@RequestMapping("/api/v1/oauth2/microsoft")
@RequiredArgsConstructor
public class MicrosoftOAuth2Controller {

    private final MicrosoftOAuth2Service microsoftOAuth2Service;

    /**
     * Inicia o fluxo de autorização OAuth2 da Microsoft.
     * Redireciona o usuário para a página de login/consentimento da Microsoft.
     *
     * @param emailSearchConfigId ID da configuração de email
     * @return RedirectView para a URL de autorização da Microsoft
     */
    @GetMapping("/authorize/{emailSearchConfigId}")
    public RedirectView authorize(@PathVariable Long emailSearchConfigId) {
        log.info("Iniciando fluxo OAuth2 para EmailSearchConfig ID: {}", emailSearchConfigId);

        try {
            String authorizationUrl = microsoftOAuth2Service.generateAuthorizationUrl(emailSearchConfigId);
            log.info("Redirecionando para Microsoft OAuth2: {}", authorizationUrl);
            return new RedirectView(authorizationUrl);
        } catch (Exception e) {
            log.error("Erro ao gerar URL de autorização: {}", e.getMessage(), e);
            // Em caso de erro, redireciona para uma página de erro (você pode customizar)
            return new RedirectView("/error?message=Erro+ao+iniciar+autorizacao+OAuth2");
        }
    }

    /**
     * Callback do OAuth2 da Microsoft.
     * Recebe o authorization code e troca por access token e refresh token.
     *
     * @param code Authorization code retornado pela Microsoft
     * @param state Estado CSRF contendo o emailSearchConfigId
     * @param error Erro retornado pela Microsoft (se houver)
     * @param errorDescription Descrição do erro (se houver)
     * @return RedirectView para página de sucesso ou erro
     */
    @GetMapping("/callback")
    public RedirectView callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription
    ) {
        log.info("Callback OAuth2 recebido - state: {}, error: {}", state, error);

        // Verifica se houve erro na autorização
        if (error != null) {
            log.error("Erro na autorização OAuth2: {} - {}", error, errorDescription);
            String errorMessage = errorDescription != null ? errorDescription : error;
            return new RedirectView("/oauth2/error?message=" + errorMessage);
        }

        // Valida parâmetros
        if (code == null || code.isBlank() || state == null || state.isBlank()) {
            log.error("Parâmetros inválidos no callback OAuth2 - code: {}, state: {}", code, state);
            return new RedirectView("/oauth2/error?message=Parametros+invalidos");
        }

        try {
            // Troca o authorization code por tokens
            microsoftOAuth2Service.exchangeCodeForTokens(code, state);

            log.info("Autorização OAuth2 concluída com sucesso para state: {}", state);
            return new RedirectView("/oauth2/success?emailSearchConfigId=" + state);

        } catch (MailReaderException e) {
            log.error("Erro ao processar callback OAuth2: {}", e.getMessage(), e);
            return new RedirectView("/oauth2/error?message=" + e.getMessage());
        } catch (Exception e) {
            log.error("Erro inesperado ao processar callback OAuth2: {}", e.getMessage(), e);
            return new RedirectView("/oauth2/error?message=Erro+ao+processar+autorizacao");
        }
    }

    /**
     * Endpoint REST para verificar o status da autorização OAuth2.
     *
     * @param emailSearchConfigId ID da configuração de email
     * @return ResponseEntity com status da autorização
     */
    @GetMapping("/status/{emailSearchConfigId}")
    @ResponseBody
    public ResponseEntity<OAuthStatusResponse> getOAuthStatus(@PathVariable Long emailSearchConfigId) {
        // TODO: Implementar verificação de status
        // Por enquanto, retorna um placeholder
        return ResponseEntity.ok(new OAuthStatusResponse(false, "Not implemented"));
    }

    /**
     * DTO para resposta de status OAuth2
     */
    public record OAuthStatusResponse(boolean authorized, String message) {}
}
