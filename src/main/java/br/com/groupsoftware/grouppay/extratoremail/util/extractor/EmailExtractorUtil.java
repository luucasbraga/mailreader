package br.com.groupsoftware.grouppay.extratoremail.util.extractor;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.internet.MimeMultipart;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitário para extração e processamento de informações de e-mails.
 * <p>
 * Esta classe fornece métodos para extrair dados de e-mails, como nome do destinatário, valores financeiros e datas de vencimento,
 * além de processar e-mails com diferentes tipos de conteúdo (texto simples, HTML, multipart).
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */

@Slf4j
@UtilityClass
public class EmailExtractorUtil {

    public void processarEmailBody(String corpoEmail, String nomeEmitente) {
        // Extraindo apenas o nome e o sobrenome
        Pattern nomePattern = Pattern.compile("(?i)(?:ol[aá],?\\s*|para\\s*|prezado\\(a\\)\\s*|prezado\\s*|senhor(a)?\\s*|atenciosamente\\s*)?([A-Z][a-z]+\\s+[A-Z][a-z]+)");
        Matcher nomeMatcher = nomePattern.matcher(corpoEmail);
        String nomeUsuario = nomeMatcher.find() ? nomeMatcher.group(2).trim() : "Nome não encontrado";

        // Extraindo "Valor a pagar", "Total a pagar", "valor", "preço" e outros similares
        Pattern valorPattern = Pattern.compile("(?i)(total\\s*a\\s*pagar|valor\\s*a\\s*pagar|valor|preço)\\s*:?\\s*R?\\$?\\s*([\\d\\.]+,[\\d]{2}|[\\d,]+\\.?[\\d]*)");
        Matcher valorMatcher = valorPattern.matcher(corpoEmail);
        String valor = valorMatcher.find() ? valorMatcher.group(2).replace(".", "").replace(",", ".") : "0.00";

        // Extraindo a data de vencimento (com ou sem os dois pontos)
        Pattern vencimentoPattern = Pattern.compile("(?i)Vencimento\\s*:?\\s*(\\d{2}/\\d{2}/\\d{4})");
        Matcher vencimentoMatcher = vencimentoPattern.matcher(corpoEmail);
        String dataVencimento = vencimentoMatcher.find() ? vencimentoMatcher.group(1) : "Data não encontrada";

        // Exibindo os dados
        log.info("Nome do Usuário: {}", nomeUsuario);
        log.info("Valor: {}", valor);
        log.info("Data de Vencimento: {}", dataVencimento);
        log.info("Data de Emissão: {}", "" /*dataEmissao*/);
        log.info("Nome do Emitente: {}", nomeEmitente);
    }

    public void printEmail(Message message) throws Exception {
        String subject = message.getSubject();
        String from = message.getFrom()[0].toString();
        String body = getBody(message);

        log.info("==========================================================================");
        log.info("Assunto: {}", subject);
        log.info("De: {}", from);
        log.info("Para: {}", message.getAllRecipients()[0]);
        log.info("Corpo do e-mail: {}", body);
        processarEmailBody(body, from.substring(from.indexOf("@") + 1, from.indexOf(".")));
    }

    private String getBody(Message message) throws Exception {
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        } else if (message.isMimeType("text/html")) {
            String htmlContent = message.getContent().toString();
            return convertHtmlToText(htmlContent);
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            return getTextFromMimeMultipart(mimeMultipart);
        }
        return "";
    }

    private String convertHtmlToText(String html) {
        Document doc = Jsoup.parse(html);
        return doc.text();  // Extrai o texto legível do HTML
    }

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws Exception {
        StringBuilder result = new StringBuilder();
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result.append(bodyPart.getContent());
            } else if (bodyPart.isMimeType("text/html")) {
                String htmlContent = bodyPart.getContent().toString();
                result.append(convertHtmlToText(htmlContent));
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result.append(getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent()));
            }
        }
        return result.toString();
    }
}

