package br.com.groupsoftware.grouppay.extratoremail.extractor.invoice.impl;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Regex;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.ExpenseNF;
import br.com.groupsoftware.grouppay.extratoremail.extractor.ExtractorTemplate;
import br.com.groupsoftware.grouppay.extratoremail.extractor.invoice.NfExtractor;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Serviço para parsing e extração de dados de Notas Fiscais (NF).
 * <p>Esta classe implementa a lógica de extração de informações específicas de Notas Fiscais,
 * como CNPJ do emitente e destinatário, valor total da nota e chave de acesso, utilizando expressões regulares
 * específicas para cada tipo de nota.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Slf4j
@Component
class NfExtractorImpl extends ExtractorTemplate implements NfExtractor {

    public NfExtractorImpl(RepositoryFacade facade) {
        super(facade);
    }

    @Override
    public Expense getExpense(Document document) {
        Regex regex = getRegexByDocumentTypeAndRegion(document.getExpenseType(), String.valueOf(document.getCompany().getCity().getIbgeCode()));
        String text = document.getTextExtracted();

        LocalDate dataEmissao = extractDate(text, regex.getIssueDate());
        if (dataEmissao == null) {
            dataEmissao = defaultDueDate(text);
        }

        BigDecimal valorTotal = extractValueByPattern(text, regex.getTotalValue());
        if (valorTotal == null) {
            valorTotal = defaultTotalValue(text);
        }

        String cnpjCpfEmitente = extractByPattern(text, regex.getIssuerCNPJ());
        if(cnpjCpfEmitente == null || cnpjCpfEmitente.trim().isEmpty()){
            cnpjCpfEmitente = defaultIssuerCNPJ(text);
        }
        cnpjCpfEmitente = (cnpjCpfEmitente != null ? cnpjCpfEmitente.replaceAll("\\D", "") : null);

        ExpenseNF despesaNF = new ExpenseNF();
        despesaNF.setCnpjCpfEmitente(cnpjCpfEmitente);
        despesaNF.setDataEmissao(dataEmissao);
        despesaNF.setNumero(extractByPattern(document.getTextExtracted(), regex.getNumber()));
        despesaNF.setSerie(extractByPattern(document.getTextExtracted(), regex.getSerie()));
        despesaNF.setValorTotal(valorTotal);

        // Extrai a chave de acesso (44 dígitos, pode ter espaços no texto original)
        despesaNF.setChaveAcesso(extractChaveAcesso(text));

        return despesaNF;
    }
}
