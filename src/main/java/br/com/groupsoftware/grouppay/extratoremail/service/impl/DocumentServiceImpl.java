package br.com.groupsoftware.grouppay.extratoremail.service.impl;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentStage;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.Status;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;
import br.com.groupsoftware.grouppay.extratoremail.exception.MailReaderException;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import br.com.groupsoftware.grouppay.extratoremail.service.DocumentService;
import br.com.groupsoftware.grouppay.extratoremail.service.PdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * Implementação do serviço responsável pelo processamento de arquivos de documentos.
 * <p>
 * Esta classe oferece suporte para processar arquivos PDF, extraindo informações
 * e convertendo-os em instâncias de {@link Expense}.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */

@Slf4j
@Service
@RequiredArgsConstructor
class DocumentServiceImpl implements DocumentService {

    private final PdfService pdfService;
    private final RepositoryFacade repository;

    public Expense processPdfFile(Path pdf, String fileNameOrSuffix) throws MailReaderException {
        String suffix = fileNameOrSuffix.split("\\.")[1].toLowerCase();
        if (suffix.equals("pdf")) {
            return pdfService.getExpenseFromDocument(new Document());
        }
        return null;
    }

    @Override
    public void changeStatus(Document document, Status newStatus) {
        repository.document.findById(document.getId()).ifPresent(doc -> {
            doc.setStatus(newStatus);
            repository.document.save(doc);
        });
    }

    @Override
    public void changeStage(Document document, DocumentStage newStage) {
        document.setStage(newStage);
        document.addStageHistoryEntry();
        repository.document.save(document);
    }

    public boolean existsTextExtracted(Document document) {
        return repository.document.existsByTextExtracted(document.getTextExtracted());
    }
}
