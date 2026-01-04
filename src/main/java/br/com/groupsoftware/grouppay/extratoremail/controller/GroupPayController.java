package br.com.groupsoftware.grouppay.extratoremail.controller;

import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.ClientGroupDTO;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.DocumentDTO;
import br.com.groupsoftware.grouppay.extratoremail.service.ServiceFacade;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para receber e processar dados enviados pelo GroupPayCore.
 * <p>
 * Este controlador expõe endpoints para que o GroupPayCore envie despesas
 * extraídas ao MailReader, onde serão processadas.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/group-pay")
@RequiredArgsConstructor
public class GroupPayController {

    private final ServiceFacade facade;

    @Deprecated
    @Async
    @PostMapping("/expenses")
    public void receiveExpense(@RequestBody DocumentDTO documentDTO) {
        facade.groupPay.processExpense(documentDTO);
    }

    @PostMapping("/create-update-company")
    public ResponseEntity<Void> createCompany(@RequestBody ClientGroupDTO clientGroup) throws JsonProcessingException {
        log.info("Received request to create a company: {}", clientGroup);
        facade.groupPay.createUpdateCompany(clientGroup);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/create-update-client-group")
    public ResponseEntity<Void> createUpdateClientGroup(@RequestBody ClientGroupDTO clientGroup) {
        log.info("Received request to create/update client group: {}", clientGroup);
        facade.groupPay.createUpdateClientGroup(clientGroup);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/update-email/client-group/{uuidClientGroup}")
    public ResponseEntity<Void> updateEmailClientGroup(@PathVariable("uuidClientGroup") String uuidClientGroup,
                                                       @RequestParam("email") String email) {
        facade.groupPay.updateEmailClientGroup(uuidClientGroup, email);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/disable-company/{uuidCompany}")
    public ResponseEntity<Void> disableCompany(@PathVariable("uuidCompany") String uuidCompany) {
        facade.groupPay.enableDisableCompany(uuidCompany, false);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/enable-company/{uuidCompany}")
    public ResponseEntity<Void> enableCompany(@PathVariable("uuidCompany") String uuidCompany) {
        facade.groupPay.enableDisableCompany(uuidCompany, true);
        return ResponseEntity.ok().build();
    }

}
