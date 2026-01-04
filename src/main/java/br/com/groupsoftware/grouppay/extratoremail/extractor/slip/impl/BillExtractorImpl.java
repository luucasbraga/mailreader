package br.com.groupsoftware.grouppay.extratoremail.extractor.slip.impl;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Regex;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.ExpenseFatura;
import br.com.groupsoftware.grouppay.extratoremail.extractor.ExtractorTemplate;
import br.com.groupsoftware.grouppay.extratoremail.extractor.slip.BillExtractor;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Serviço para parsing e extração de dados de faturas.
 * <p>
 * Esta classe estende a {@link ExtractorTemplate} e implementa a interface {@link BillExtractor},
 * oferecendo uma estrutura para a extração de informações relevantes de faturas. Os métodos implementados
 * permitem extrair dados como nome do emissor, data de emissão, data de vencimento, CNPJ do emissor,
 * CNPJ do destinatário, valor total e chave de acesso a partir do texto extraído do documento.
 * </p>
 * <p>
 * A implementação atual funciona como um esqueleto para futuras customizações, servindo de base para a
 * adaptação conforme as regras de negócio e os formatos específicos dos documentos de fatura.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2025
 */
@Slf4j
@Component
public class BillExtractorImpl extends ExtractorTemplate implements BillExtractor {

    public BillExtractorImpl(RepositoryFacade facade) {
        super(facade);
    }

    @Override
    public Expense getExpense(Document document) {
        Regex regex = getRegexByDocument(document);
        LocalDate dataEmissao = extractDate(document.getTextExtracted(), regex.getIssueDate());
        String cnpjEmissor = extractByPattern(document.getTextExtracted(), regex.getIssuerCNPJ());

        ExpenseFatura despesaFatura = new ExpenseFatura();
        despesaFatura.setCnpjCpfEmitente(cnpjEmissor);
        despesaFatura.setDataEmissao(dataEmissao);
        despesaFatura.setNumero(extractByPattern(document.getTextExtracted(), regex.getNumber()));
        despesaFatura.setValorTotal(extractValueByPattern(document.getTextExtracted(), regex.getTotalValue()));
        despesaFatura.setEmitente(cnpjEmissor);
        despesaFatura.setDataEmissao(dataEmissao);

        return despesaFatura;
    }
}
