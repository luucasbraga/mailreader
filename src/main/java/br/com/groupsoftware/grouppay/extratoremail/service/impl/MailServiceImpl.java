package br.com.groupsoftware.grouppay.extratoremail.service.impl;

import br.com.groupsoftware.grouppay.extratoremail.exception.MailReaderException;
import br.com.groupsoftware.grouppay.extratoremail.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

@Slf4j
@RequiredArgsConstructor
@Service
public class MailServiceImpl implements MailService {

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private String port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${spring.mail.from}")
    private String from;

    @Override
    public void sendMail(String to, String subject, String template, HashMap<String, Object> parameters) throws MailReaderException {
        log.info("[SEND_MAIL] ENVIANDO EMAIL \"{}\"", subject);
        Session session = createSession();

        try {
            Message mimeMessage = new MimeMessage(session);

            Locale locale = Locale.getDefault();
            Context context = new Context(locale);
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                context.setVariable(entry.getKey(),entry.getValue());
            }

            String content = templateEngine.process(template, context);
            mimeMessage.setFrom(new InternetAddress(from));
            mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            mimeMessage.setSubject(subject);
            mimeMessage.setContent(content, "text/html; charset=UTF-8");

            Transport.send(mimeMessage);
            log.info("[SEND_MAIL] EMAIL ENVIADO PARA '{}'", to);
        } catch (Exception e) {
            log.info("[SEND_MAIL] ERRO AO ENVIAR EMAIL \"{}\"", subject);
            e.printStackTrace();
            if (log.isDebugEnabled()) {
                log.warn("Email could not be sent to user '{}'", to, e);
            } else {
                log.error("Email could not be sent to user '{}': {}", to, e.getMessage());
            }
            throw new MailReaderException("Erro ao enviar email. error: " + e.getMessage());
        }
    }

    @Override
    public void sendMailWithAttachment(String to, String subject, String body, String attachmentFileName,
                                       byte[] attachmentContent, String attachmentContentType) throws MailReaderException {
        log.info("[SEND_MAIL] ENVIANDO EMAIL COM ANEXO \"{}\"", subject);
        Session session = createSession();

        try {
            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(from));
            mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            mimeMessage.setSubject(subject);

            MimeBodyPart bodyPart = new MimeBodyPart();
            bodyPart.setText(body, StandardCharsets.UTF_8.name());

            MimeBodyPart attachmentPart = new MimeBodyPart();
            String safeFileName = (attachmentFileName == null || attachmentFileName.isBlank()) ? "arquivo_confirmacao" : attachmentFileName;
            String safeContentType = (attachmentContentType == null || attachmentContentType.isBlank()) ? "application/octet-stream" : attachmentContentType;
            attachmentPart.setFileName(safeFileName);
            attachmentPart.setDataHandler(new DataHandler(new ByteArrayDataSource(attachmentContent, safeContentType)));

            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(bodyPart);
            multipart.addBodyPart(attachmentPart);

            mimeMessage.setContent(multipart);

            Transport.send(mimeMessage);
            log.info("[SEND_MAIL] EMAIL COM ANEXO ENVIADO PARA '{}'", to);
        } catch (Exception e) {
            log.info("[SEND_MAIL] ERRO AO ENVIAR EMAIL COM ANEXO \"{}\"", subject);
            if (log.isDebugEnabled()) {
                log.warn("Email with attachment could not be sent to user '{}'", to, e);
            } else {
                log.error("Email with attachment could not be sent to user '{}': {}", to, e.getMessage());
            }
            throw new MailReaderException("Erro ao enviar email com anexo. error: " + e.getMessage());
        }
    }

    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }
}
