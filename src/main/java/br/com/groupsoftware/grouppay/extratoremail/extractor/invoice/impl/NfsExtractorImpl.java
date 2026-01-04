package br.com.groupsoftware.grouppay.extratoremail.extractor.invoice.impl;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Regex;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.ExpenseNFS;
import br.com.groupsoftware.grouppay.extratoremail.extractor.ExtractorTemplate;
import br.com.groupsoftware.grouppay.extratoremail.extractor.invoice.NfsExtractor;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Serviço para parsing e extração de dados de Notas Fiscais de Serviço (NFS-e).
 * Esta classe implementa métodos para extrair informações específicas de NFS-e,
 * como CNPJ do prestador e tomador, datas de emissão e vencimento, e o valor total.
 *
 * <p>Extende a {@link ExtractorTemplate} e implementa a interface {@link NfsExtractor}
 * para fornecer uma solução especializada de extração de dados para notas fiscais de serviço.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Slf4j
@Component
class NfsExtractorImpl extends ExtractorTemplate implements NfsExtractor {

    public NfsExtractorImpl(RepositoryFacade facade) {
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

        ExpenseNFS despesaNFS = new ExpenseNFS();
        despesaNFS.setCnpjCpfEmitente(cnpjCpfEmitente);
        despesaNFS.setDataEmissao(dataEmissao);
        despesaNFS.setNumero(extractByPattern(text, regex.getNumber()));
        despesaNFS.setSerie(extractByPattern(text, regex.getSerie()));
        despesaNFS.setValorTotal(valorTotal);

        return despesaNFS;
    }
}