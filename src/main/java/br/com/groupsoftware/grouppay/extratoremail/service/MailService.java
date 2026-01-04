package br.com.groupsoftware.grouppay.extratoremail.service;

import br.com.groupsoftware.grouppay.extratoremail.exception.MailReaderException;

import java.util.HashMap;

public interface MailService {

    void sendMail(String to, String subject, String template, HashMap<String, Object> parameters) throws MailReaderException;

    void sendMailWithAttachment(String to, String subject, String body, String attachmentFileName,
                                byte[] attachmentContent, String attachmentContentType) throws MailReaderException;

}
