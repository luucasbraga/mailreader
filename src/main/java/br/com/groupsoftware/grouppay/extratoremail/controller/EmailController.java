package br.com.groupsoftware.grouppay.extratoremail.controller;

import br.com.groupsoftware.grouppay.extratoremail.exception.MailReaderException;
import br.com.groupsoftware.grouppay.extratoremail.service.ServiceFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import java.util.List;

/**
 * Controlador REST para gerenciamento e processamento de e-mails.
 * <p>
 * Fornece endpoints para operações relacionadas ao salvamento e processamento de anexos de e-mails.
 * Atualmente, o código comentado sugere a possibilidade de implementar a funcionalidade de
 * salvamento de PDFs a partir de e-mails.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
public class EmailController {

    @Value("${authApiKeyMailValidation}")
    private String authApiKeyMailValidation;

    private final ServiceFacade service;

    @PostMapping("/validation/redirect-success")
    public ResponseEntity<Void> receberConfirmacaoTesteEmail(@RequestHeader(value="apiKey", required = true) String apiKey,
                                                               @RequestParam (name = "uuidClientGroup") String uuidClientGroup, 
                                                               @RequestParam("email") String email) {
        if (apiKey == null || !apiKey.equals(authApiKeyMailValidation)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        if (uuidClientGroup == null || uuidClientGroup.isBlank() || email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        service.email.saveSendRedirectSuccess(uuidClientGroup, email);
        return ResponseEntity.ok(null);
    }

    @GetMapping("/test-redirect-email/{codigoSuporte}")
    public ResponseEntity<Void> testRedirectEmail(@PathVariable("codigoSuporte") String codigoSuporte) throws MailReaderException, MessagingException {
        service.groupPay.sendMailTest(codigoSuporte);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/confirmar-redirecionamento/{codigoSuporte}")
    public ResponseEntity<Void> confirmarRedirecionamento(@PathVariable("codigoSuporte") String codigoSuporte) throws MailReaderException {
        service.email.processRedirectConfirmation(codigoSuporte);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/access-logs/{email}")
    public ResponseEntity<List<br.com.groupsoftware.grouppay.extratoremail.domain.entity.EmailAccessLog>> getEmailAccessLogs(@PathVariable("email") String email) {
        var logs = service.email.getEmailAccessLogsByEmail(email);
        return ResponseEntity.ok(logs);
    }
}
