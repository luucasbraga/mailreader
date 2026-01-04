package br.com.groupsoftware.grouppay.extratoremail.service.impl;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentStage;
import br.com.groupsoftware.grouppay.extratoremail.service.DecryptPdfService;
import br.com.groupsoftware.grouppay.extratoremail.service.DocumentService;
import br.com.groupsoftware.grouppay.extratoremail.util.document.PdfPasswordUtil;
import br.com.groupsoftware.grouppay.extratoremail.util.file.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Implementação do serviço para descriptografia de arquivos PDF.
 * <p>
 * Esta classe processa arquivos PDF que podem estar protegidos por senha.
 * Se o PDF estiver criptografado, a senha é removida e o arquivo é armazenado
 * em um diretório seguro. Se o PDF não estiver protegido, ele é copiado para
 * o diretório final sem modificações.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
class DecryptPdfServiceImpl implements DecryptPdfService {

    @Value("${reader.dir}")
    private String readerDir;

    @Value("${reader.download}")
    private String readerDownload;

    private final Environment environment;
    private final DocumentService documentService;

    @Override
    public void decryptPdf(Document document) {
        if (document != null) {
            Path pdfPath = Paths.get(readerDir, readerDownload, document.getFileName());
            try {
                if (decryptAndProcessPdf(pdfPath, document)) {
                    documentService.changeStage(document, DocumentStage.PASSWORD_REMOVED);
                }
            } catch (Exception e) {
                log.error("Erro ao processar ou deletar PDF: {}", pdfPath, e);
            }
        }
    }

    private Boolean decryptAndProcessPdf(Path pdfPath, Document document) throws Exception {
        String email;
        if (Objects.nonNull(document.getClientGroup())) {
            email = document.getClientGroup().getEmail();
        }
        else {
            email = document.getCompany().getEmail();
        }

        Path decryptedDirectory = Paths.get(readerDir, email);
        FileUtils.createDirectoryIfNotExists(decryptedDirectory);

        Path tempDecryptedPdfPath = Files.createTempFile("decrypted_", ".pdf"); // Arquivo temporário

        try (InputStream inputStream = Files.newInputStream(pdfPath)) {
            if (PdfPasswordUtil.isPdfEncrypted(pdfPath.toString())) {
                boolean success = PdfPasswordUtil.crackAndRemovePassword(pdfPath.toString(), tempDecryptedPdfPath.toString(), environment);
                if (success) {
                    Path finalDecryptedPdfPath = decryptedDirectory.resolve(pdfPath.getFileName());
                    Files.move(tempDecryptedPdfPath, finalDecryptedPdfPath, StandardCopyOption.REPLACE_EXISTING);
                    log.info("PDF descriptografado e salvo em: {}", finalDecryptedPdfPath);
                    return true;
                } else {
                    log.error("Falha ao descriptografar o PDF: {}", pdfPath);
                    Files.deleteIfExists(tempDecryptedPdfPath); // Remove o temporário se falhar
                    return false;
                }
            } else {
                Path finalDecryptedPdfPath = decryptedDirectory.resolve(pdfPath.getFileName());
                Files.copy(inputStream, finalDecryptedPdfPath, StandardCopyOption.REPLACE_EXISTING);
                log.info("PDF copiado sem senha em: {}", finalDecryptedPdfPath);
                return true;
            }
        } catch (IOException e) {
            log.error("Erro ao processar o PDF: {}", e.getMessage(), e);
            Files.deleteIfExists(tempDecryptedPdfPath); // Remove o temporário em caso de erro
            throw e;
        }
    }
}

