package br.com.groupsoftware.grouppay.extratoremail.service.impl;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentExtractorType;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.DocumentStage;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.ExpenseType;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;
import br.com.groupsoftware.grouppay.extratoremail.exception.MailReaderException;
import br.com.groupsoftware.grouppay.extratoremail.extractor.core.ExtractorFacade;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import br.com.groupsoftware.grouppay.extratoremail.service.PdfService;
import br.com.groupsoftware.grouppay.extratoremail.util.DocumentUtils;
import br.com.groupsoftware.grouppay.extratoremail.util.brazil.CpfCnpjUtil;
import br.com.groupsoftware.grouppay.extratoremail.util.document.PdfTypeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação do serviço de processamento de arquivos PDF e extração de dados financeiros.
 *
 * <p>Este serviço é responsável por identificar o tipo de documento, extrair conteúdo de arquivos PDF e
 * processar os dados financeiros encontrados. Utiliza diferentes estratégias de extração de acordo com
 * o tipo de documento e a configuração da empresa associada ao documento.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */

@Slf4j
@Service
@RequiredArgsConstructor
class PdfServiceImpl implements PdfService {

    private final ExtractorFacade extractor;
    private final RepositoryFacade repository;

    @Override
    public Expense getExpenseFromDocument(Document document) throws MailReaderException {
        if (document == null || (document.getCompany() == null && document.getClientGroup() == null)) {
            throw new MailReaderException("Documento ou empresa não pode ser nulo.");
        }

        Expense expense = processDocument(document);
        if (expense != null) {
            updateEmitenteInfo(expense);
            expense.setExpenseType(document.getExpenseType());
            expense.setDataEmissao(expense.getDataEmissao() == null ? LocalDate.now() : expense.getDataEmissao());
        }
        repository.document.save(document);
        return expense;
    }


    private void updateEmitenteInfo(Expense expense) {
        String originalCnpjCpf = expense.getCnpjCpfEmitente();
        if (!isNullOrEmpty(originalCnpjCpf) && !"null".equalsIgnoreCase(originalCnpjCpf.trim())) {
            String sanitized = originalCnpjCpf.replaceAll("\\D", "");
            if (CpfCnpjUtil.isCnpjCpfValid(sanitized)) {
                expense.setCnpjCpfEmitente(sanitized);
                if (isNullOrEmpty(expense.getEmitente()) || "null".equalsIgnoreCase(expense.getEmitente().trim())) {
                    expense.setEmitente(sanitized);
                }
            } else {
                log.warn("CNPJ/CPF emitente inválido após sanitização: {}", sanitized);
                expense.setCnpjCpfEmitente(null);
                expense.setEmitente(null);
            }
        } else {
            expense.setCnpjCpfEmitente(null);
            expense.setEmitente(null);
        }
    }

    private String sanitize(String value) {
        return value != null ? value.replaceAll("\\D", "") : null;
    }

    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Expense processDocument(Document document) {
        ExpenseType type = PdfTypeUtil.identificarTipoPdf(document.getTextExtracted());
        document.setExpenseType(type);
        if (ExpenseType.OUTRO.equals(type)) {
            processUnknownDocumentType(document, type);
            return null;
        }
        if (DocumentUtils.isAiUser(document)) {
            log.info("Tipo de documento identificado: {}", type);
            return processAIDocument(document, type);
        }
        return processRegexDocument(document, type);
    }

    private Expense processAIDocument(Document document, ExpenseType type) {
        List<DocumentExtractorType> extractorTypes = new ArrayList<>(document.getDocumentExtractorTypes());
        extractorTypes.add(DocumentExtractorType.OPENAI);
        document.setDocumentExtractorTypes(extractorTypes);
        return extractor.openAi.getExpense(document, type);
    }

    private Expense processRegexDocument(Document document, ExpenseType type) {
        return switch (type) {
            case NFE -> {
                log.info("Processando NF-e...");
                yield extractor.invoice.nf.getExpense(document);
            }
            case NFSE -> {
                log.info("Processando NFS-e...");
                yield extractor.invoice.nfs.getExpense(document);
            }
            case NFCE -> {
                log.info("Processando NFC-e...");
                yield extractor.invoice.nfc.getExpense(document);
            }
            case NF3E -> {
                log.info("Processando NF3-e...");
                yield extractor.invoice.nf3.getExpense(document);
            }
            case CTE -> {
                log.info("Processando CT-e...");
                yield extractor.invoice.ct.getExpense(document);
            }
            case BOLETO -> {
                log.info("Processando Boleto...");
                yield extractor.slip.bank.getExpense(document);
            }
            case FATURA -> {
                log.info("Processando Fatura...");
                yield extractor.slip.bill.getExpense(document);
            }
            default -> null;
        };
    }

    protected void processUnknownDocumentType(Document document, ExpenseType type) {
        log.warn("Tipo de documento desconhecido: {}", type);
        document.setStage(DocumentStage.DELETE_FROM_LOCAL);
        document.addStageHistoryEntry();
    }
}
