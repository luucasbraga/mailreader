package br.com.groupsoftware.grouppay.extratoremail.extractor.slip.impl;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Regex;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.ExpenseBoleto;
import br.com.groupsoftware.grouppay.extratoremail.extractor.ExtractorTemplate;
import br.com.groupsoftware.grouppay.extratoremail.extractor.slip.BankSlipExtractor;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Serviço para parsing e extração de dados de Boletos Bancários.
 * <p>
 * Esta classe estende a {@link ExtractorTemplate} e implementa a interface {@link BankSlipExtractor},
 * fornecendo uma estrutura para a extração de informações relevantes dos boletos bancários.
 * Os métodos implementados permitem extrair dados como nome do emissor, data de emissão, data de vencimento,
 * CNPJ do emissor, CNPJ do destinatário, valor total e chave de acesso a partir do texto do documento.
 * </p>
 * <p>
 * A implementação atual serve como esqueleto para futuras customizações, conforme as regras de negócio
 * e os formatos específicos dos boletos.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2025
 */
@Slf4j
@Component
public class BankExtractorImpl extends ExtractorTemplate implements BankSlipExtractor {

    public BankExtractorImpl(RepositoryFacade facade) {
        super(facade);
    }

    @Override
    public Expense getExpense(Document document) {
        Regex regex = getRegexByDocument(document);
        LocalDate dataEmissao = extractDate(document.getTextExtracted(), regex.getIssueDate());
        String cnpjEmissor = extractByPattern(document.getTextExtracted(), regex.getIssuerCNPJ());

        ExpenseBoleto despesaBoleto = new ExpenseBoleto();
        despesaBoleto.setCnpjCpfEmitente(cnpjEmissor);
        despesaBoleto.setDataEmissao(dataEmissao);
        despesaBoleto.setNumero(extractByPattern(document.getTextExtracted(), regex.getNumber()));
        despesaBoleto.setValorTotal(extractValueByPattern(document.getTextExtracted(), regex.getTotalValue()));
        despesaBoleto.setEmitente(cnpjEmissor);
        despesaBoleto.setDataEmissao(dataEmissao);

        return despesaBoleto;
    }
}
