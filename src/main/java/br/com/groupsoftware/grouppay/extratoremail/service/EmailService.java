package br.com.groupsoftware.grouppay.extratoremail.service;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.ClientGroup;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.EmailAccessLog;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.EmailSearchConfig;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.ReasonAccessType;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.RedirectStatusTestType;
import br.com.groupsoftware.grouppay.extratoremail.exception.MailReaderException;

import javax.mail.MessagingException;
import javax.mail.Store;
import java.util.List;
import java.util.Properties;

/**
 * Interface para o serviço de gerenciamento e busca de e-mails.
 * <p>
 * Define o contrato para a busca de e-mails com anexos e seu processamento a partir de uma entidade {@link ClientGroup}.
 * O objetivo é facilitar a integração com sistemas de e-mail para buscar mensagens e salvar arquivos PDF anexados.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
public interface EmailService {
    void getEmailsAndSavePdfs(ClientGroup clientGroup) throws Exception;

    void saveSendRedirectSuccess(String uuidClientGroup, String email);

    boolean sendValidationStatusToGroupPayCore(ClientGroup clientGroup, String email, RedirectStatusTestType status) throws MailReaderException;

    Properties getEmailProperties(EmailSearchConfig emailSearchConfig, boolean usarOAuth2MicrosftAzure);

    Store connectToEmailStore(Properties properties, EmailSearchConfig emailSearchConfig, boolean usarOAuth2MicrosftAzure) throws MessagingException;

    void registerAccessLog(String email, ReasonAccessType reason);

    boolean usarOAuth2MicrosftAzure(String email);

    void processRedirectConfirmation(String codigoSuporte) throws MailReaderException;

    List<EmailAccessLog> getEmailAccessLogsByEmail(String email);
}
