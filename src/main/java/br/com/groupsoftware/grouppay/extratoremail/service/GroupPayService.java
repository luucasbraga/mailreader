package br.com.groupsoftware.grouppay.extratoremail.service;

import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.ClientGroupDTO;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.dto.DocumentDTO;
import br.com.groupsoftware.grouppay.extratoremail.exception.MailReaderException;
import com.fasterxml.jackson.core.JsonProcessingException;

import javax.mail.MessagingException;

/**
 * Serviço para processar dados e ações relacionados ao GroupPay.
 * <p>
 * Esta interface define as operações de negócio necessárias para manipular
 * documentos e gerenciar usuários do sistema, incluindo a criação e
 * inativação de usuários, bem como o processamento de despesas.
 * </p>
 *
 * @author Marco
 * @version 1.0
 * @since 2024
 */
public interface GroupPayService {
    void processExpense(DocumentDTO documentDTO);

    void createUpdateCompany(ClientGroupDTO clientGroupDTO) throws JsonProcessingException;

    void createUpdateClientGroup(ClientGroupDTO clientGroupDTO);

    void updateEmailClientGroup(String uuidClientGroup, String email);

    void sendMailTest(String codigoSuporte) throws MailReaderException, MessagingException;

    void enableDisableCompany(String uuidCompany, boolean enable);
}

