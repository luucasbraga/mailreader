package br.com.groupsoftware.grouppay.extratoremail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Facade para centralizar o acesso aos services.
 * <p>
 * Esta classe serve como um ponto de acesso unificado para os diferentes serviços
 * da aplicação, facilitando a injeção de dependências e o uso de múltiplos serviços em
 * um único ponto.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Component
@RequiredArgsConstructor
public class ServiceFacade {
    public final DocumentService document;
    public final EmailService email;
    public final DecryptPdfService decryptPdf;
    public final PdfService pdf;
    public final S3DownloadService s3Download;
    public final S3UploadService s3Upload;
    public final ExpenseMapperService expenseMapper;
    public final UserService user;
    public final ExpenseSenderService expenseSender;
    public final GroupPayService groupPay;
    public final MailService mail;
}
