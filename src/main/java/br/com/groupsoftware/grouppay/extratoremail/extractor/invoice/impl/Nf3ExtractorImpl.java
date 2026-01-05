package br.com.groupsoftware.grouppay.extratoremail.extractor.invoice.impl;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Regex;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.ExpenseNF3;
import br.com.groupsoftware.grouppay.extratoremail.extractor.ExtractorTemplate;
import br.com.groupsoftware.grouppay.extratoremail.extractor.invoice.Nf3Extractor;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Serviço para parsing e extração de dados de Notas Fiscais de Energia Elétrica (NF³-e).
 * Implementa a lógica de extração de informações específicas de NF³-e, como CNPJ do emitente e destinatário,
 * valor total da nota e datas relevantes.
 *
 * <p>Esta classe estende a {@link ExtractorTemplate} e implementa a interface {@link Nf3Extractor},
 * fornecendo uma extração customizada para notas fiscais de energia elétrica.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Slf4j
@Component
class Nf3ExtractorImpl extends ExtractorTemplate implements Nf3Extractor {

    public Nf3ExtractorImpl(RepositoryFacade facade) {
        super(facade);
    }

    @Override
    public Expense getExpense(Document document) {
        String text = document.getTextExtracted();
        Regex regex = getRegexByDocument(document);
        LocalDate dataEmissao = extractDate(text, regex.getIssueDate());
        String cnpjEmissor = extractByPattern(text, regex.getIssuerCNPJ());

        ExpenseNF3 despesaNF3 = new ExpenseNF3();
        despesaNF3.setCnpjCpfEmitente(cnpjEmissor);
        despesaNF3.setDataEmissao(dataEmissao);
        despesaNF3.setNumero(extractByPattern(text, regex.getNumber()));
        despesaNF3.setSerie(extractByPattern(text, regex.getSerie()));
        despesaNF3.setValorTotal(extractValueByPattern(text, regex.getTotalValue()));
        despesaNF3.setEmitente(cnpjEmissor);

        // Extrai a chave de acesso (44 dígitos, pode ter espaços no texto original)
        despesaNF3.setChaveAcesso(extractChaveAcesso(text));

        return despesaNF3;
    }
}
