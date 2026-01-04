package br.com.groupsoftware.grouppay.extratoremail.extractor.invoice.impl;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Regex;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.ExpenseNFC;
import br.com.groupsoftware.grouppay.extratoremail.extractor.ExtractorTemplate;
import br.com.groupsoftware.grouppay.extratoremail.extractor.invoice.NfcExtractor;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Serviço para parsing e extração de dados de Notas Fiscais do tipo NFC.
 * <p>
 * Esta classe estende a {@link ExtractorTemplate} e implementa a interface {@link NfcExtractor},
 * oferecendo uma estrutura para a extração de informações relevantes de documentos fiscais do tipo NFC.
 * Os métodos implementados permitem a extração de dados como nome do emissor, data de emissão, data de vencimento,
 * CNPJ do emissor, CNPJ do destinatário, valor total e chave de acesso a partir do texto extraído do documento.
 * </p>
 * <p>
 * A implementação atual funciona como um esqueleto, servindo de base para futuras customizações de acordo com as
 * regras de negócio e os formatos específicos dos documentos NFC.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2025
 */
@Slf4j
@Component
public class NfcExtractorImpl extends ExtractorTemplate implements NfcExtractor {

    public NfcExtractorImpl(RepositoryFacade facade) {
        super(facade);
    }

    @Override
    public Expense getExpense(Document document) {
        Regex regex = getRegexByDocument(document);
        LocalDate dataEmissao = extractDate(document.getTextExtracted(), regex.getIssueDate());
        String cnpjEmissor = extractByPattern(document.getTextExtracted(), regex.getIssuerCNPJ());

        ExpenseNFC despesaNFC = new ExpenseNFC();
        despesaNFC.setCnpjCpfEmitente(cnpjEmissor);
        despesaNFC.setDataEmissao(dataEmissao);
        despesaNFC.setNumero(extractByPattern(document.getTextExtracted(), regex.getNumber()));
        despesaNFC.setSerie(extractByPattern(document.getTextExtracted(), regex.getSerie()));
        despesaNFC.setValorTotal(extractValueByPattern(document.getTextExtracted(), regex.getTotalValue()));
        despesaNFC.setEmitente(cnpjEmissor);
        despesaNFC.setDataEmissao(dataEmissao);

        return despesaNFC;
    }
}
