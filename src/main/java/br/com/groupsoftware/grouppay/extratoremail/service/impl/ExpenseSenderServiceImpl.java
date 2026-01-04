package br.com.groupsoftware.grouppay.extratoremail.service.impl;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentStage;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.DocumentDTO;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.ExpenseDTO;
import br.com.groupsoftware.grouppay.extratoremail.security.GroupPayTokenManager;
import br.com.groupsoftware.grouppay.extratoremail.service.DocumentService;
import br.com.groupsoftware.grouppay.extratoremail.service.ExpenseSenderService;
import br.com.groupsoftware.grouppay.extratoremail.util.RestUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@RequiredArgsConstructor
@Slf4j
@Service
class ExpenseSenderServiceImpl implements ExpenseSenderService {

    private final RestTemplate restTemplate;
    private final GroupPayTokenManager tokenManager;
    private final DocumentService documentService;

    @Value("${group-pay.core}")
    private String host;

    private static final String BASE_PATH = "/api/v1/configuracao-mail-reader";
    private static final String PATH_ENVIO_DOCUMENTO = "/receber-documento";
    private static final String PATH_CONSULTA_RESPOSTA = "/resposta-expense/";

    /**
     * Envia a despesa para o endpoint do Group Pay.
     *
     * @param document   Documento associado à despesa.
     * @param expenseDTO Dados da despesa.
     */
    public void sendExpense(Document document, ExpenseDTO expenseDTO) {
        try {
            String token = tokenManager.getToken(document.getCompany().getClientGroup());
            HttpHeaders headers = RestUtil.createAuthHeaders(token);

            HttpEntity<ExpenseDTO> request = new HttpEntity<>(expenseDTO, headers);
            String url = RestUtil.buildUrl(host, BASE_PATH, PATH_ENVIO_DOCUMENTO, null);
            ResponseEntity<Void> response = restTemplate.postForEntity(url, request, Void.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                documentService.changeStage(document, DocumentStage.SENT_TO_GROUP_PAY);
                log.info("Despesa enviada com sucesso: {}", expenseDTO);
            } else if (response.getStatusCode() == HttpStatus.UNAUTHORIZED || response.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.warn("Token expirado. Renovando token para documento ID: {}", document.getId());
                tokenManager.renewToken(document.getCompany().getClientGroup());
                sendExpense(document, expenseDTO);
            } else {
                log.error("Erro ao enviar despesa para o documento ID: {}. Status: {}", document.getId(), response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Erro ao enviar despesa para o documento ID: {}. Erro: {}", document.getId(), e.getMessage(), e);
        }
    }

    /**
     * Verifica a resposta do processamento da despesa no endpoint do Group Pay.
     *
     * @param document Documento associado.
     * @return A resposta como String, ou null em caso de falha.
     */
    public DocumentDTO getResultExpense(Document document) {
        Long documentId = document.getId();
        try {
            String token = tokenManager.getToken(document.getCompany().getClientGroup());
            HttpHeaders headers = RestUtil.createAuthHeaders(token);

            String url = RestUtil.buildUrl(host, BASE_PATH, PATH_CONSULTA_RESPOSTA, documentId);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<DocumentDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    DocumentDTO.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Resposta obtida com sucesso para documento ID: {}", documentId);
                return response.getBody();
            } else if (response.getStatusCode() == HttpStatus.UNAUTHORIZED ||
                    response.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.warn("Token expirado na consulta de resposta para documento ID: {}. Renovando token.", documentId);
                tokenManager.renewToken(document.getCompany().getClientGroup());
                return getResultExpense(document);
            } else {
                log.warn("Falha ao consultar resposta para documento ID: {}. Status HTTP: {}",
                        documentId, response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Erro ao consultar resposta para documento ID: {}. Erro: {}", documentId, e.getMessage(), e);
            return null;
        }
    }


}
