package br.com.groupsoftware.grouppay.extratoremail.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controlador para renderizar páginas de sucesso e erro do OAuth2.
 *
 * @author Claude
 * @version 1.0
 * @since 2026
 */
@Controller
@RequestMapping("/oauth2")
public class OAuth2ViewController {

    /**
     * Página de sucesso após autorização OAuth2.
     *
     * @param emailSearchConfigId ID da configuração de email autorizada
     * @param model Model do Thymeleaf
     * @return Nome do template
     */
    @GetMapping("/success")
    public String success(@RequestParam(required = false) String emailSearchConfigId, Model model) {
        if (emailSearchConfigId != null) {
            model.addAttribute("emailSearchConfigId", emailSearchConfigId);
        }
        return "oauth2-success";
    }

    /**
     * Página de erro após falha na autorização OAuth2.
     *
     * @param message Mensagem de erro
     * @param model Model do Thymeleaf
     * @return Nome do template
     */
    @GetMapping("/error")
    public String error(@RequestParam(required = false) String message, Model model) {
        if (message != null) {
            model.addAttribute("message", message);
        }
        return "oauth2-error";
    }
}
