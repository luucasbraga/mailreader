package br.com.groupsoftware.grouppay.extratoremail.extractor.invoice.impl;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Regex;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.ExpenseCT;
import br.com.groupsoftware.grouppay.extratoremail.extractor.ExtractorTemplate;
import br.com.groupsoftware.grouppay.extratoremail.extractor.invoice.CtExtractor;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Serviço para parsing e extração de dados de Notas Fiscais do tipo CT.
 * <p>
 * Esta classe estende a {@link ExtractorTemplate} e implementa a interface {@link CtExtractor},
 * oferecendo uma estrutura para a extração de informações relevantes de documentos fiscais do tipo CT.
 * Os métodos definidos permitem a extração de dados como nome do emissor, data de emissão, data de vencimento,
 * CNPJ do emissor, CNPJ do destinatário, valor total e chave de acesso.
 * </p>
 * <p>
 * A implementação atual funciona como um esqueleto, servindo de base para futuras customizações
 * conforme as regras de negócio e os formatos específicos dos documentos CT.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2025
 */
@Slf4j
@Component
public class CtExtractorImpl extends ExtractorTemplate implements CtExtractor {

    public CtExtractorImpl(RepositoryFacade facade) {
        super(facade);
    }

    @Override
    public Expense getExpense(Document document) {
        Regex regex = getRegexByDocument(document);
        LocalDate dataEmissao = extractDate(document.getTextExtracted(), regex.getIssueDate());
        String cnpjEmissor = extractByPattern(document.getTextExtracted(), regex.getIssuerCNPJ());

        ExpenseCT despesaCT = new ExpenseCT();
        despesaCT.setCnpjCpfEmitente(cnpjEmissor);
        despesaCT.setDataEmissao(dataEmissao);
        despesaCT.setNumero(extractByPattern(document.getTextExtracted(), regex.getNumber()));
        despesaCT.setSerie(extractByPattern(document.getTextExtracted(), regex.getSerie()));
        despesaCT.setValorTotal(extractValueByPattern(document.getTextExtracted(), regex.getTotalValue()));
        despesaCT.setEmitente(cnpjEmissor);
        despesaCT.setDataEmissao(dataEmissao);

        return despesaCT;
    }
}
