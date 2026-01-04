package br.com.groupsoftware.grouppay.extratoremail.extractor.slip.impl;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Regex;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.ExpenseFGTS;
import br.com.groupsoftware.grouppay.extratoremail.extractor.ExtractorTemplate;
import br.com.groupsoftware.grouppay.extratoremail.extractor.slip.FgtsExtractor;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementação para extração de dados de guias FGTS (Guia do FGTS Digital - GFD).
 * <p>
 * Esta classe extende {@link ExtractorTemplate} e implementa {@link FgtsExtractor},
 * fornecendo lógica para extrair informações como CNPJ do empregador, razão social,
 * identificador da guia, data de vencimento, valor total, código de barras e composição por competência.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2025
 */
@Slf4j
@Component
public class FgtsExtractorImpl extends ExtractorTemplate implements FgtsExtractor {

    public FgtsExtractorImpl(RepositoryFacade facade) {
        super(facade);
    }

    @Override
    public Expense getExpense(Document document) {
        String text = document.getTextExtracted();
        Regex regex = getRegexByDocument(document);

        ExpenseFGTS fgts = new ExpenseFGTS();

        // Extração de campos básicos usando regex se disponível
        if (regex != null) {
            fgts.setCnpjCpfEmitente(extractByPattern(text, regex.getIssuerCNPJ()));
            fgts.setDataEmissao(extractDate(text, regex.getIssueDate()));
            fgts.setDataVencimento(extractDate(text, regex.getDueDate()));
            fgts.setValorTotal(extractValueByPattern(text, regex.getTotalValue()));
            fgts.setNumero(extractByPattern(text, regex.getNumber()));
        }

        // Fallback para extração com métodos default e regex específicos
        if (fgts.getCnpjCpfEmitente() == null) {
            fgts.setCnpjCpfEmitente(extractCnpjEmpregador(text));
        }

        if (fgts.getDataVencimento() == null) {
            fgts.setDataVencimento(extractDataVencimento(text));
        }

        if (fgts.getValorTotal() == null) {
            fgts.setValorTotal(extractValorTotal(text));
        }

        // Extração de campos específicos do FGTS
        fgts.setRazaoSocialEmpregador(extractRazaoSocial(text));
        fgts.setIdentificador(extractIdentificador(text));
        fgts.setCodigoBarras(extractCodigoBarras(text));
        fgts.setPixCopiaCola(extractPixCopiaCola(text));
        fgts.setComposicao(extractComposicaoFgts(text));

        // Emitente é a Caixa Econômica Federal
        fgts.setEmitente("Caixa Econômica Federal");

        return fgts;
    }

    /**
     * Extrai o CNPJ do empregador.
     */
    private String extractCnpjEmpregador(String text) {
        // Tenta extrair CNPJ após "CPF/CNPJ do Empregador" ou similar
        Pattern pattern = Pattern.compile("(?:CPF/CNPJ.*Empregador|Empregador)[:\\s]*([0-9./-]{14,20})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("[^0-9]", "");
        }

        // Fallback para primeiro CNPJ encontrado
        return defaultIssuerCNPJ(text);
    }

    /**
     * Extrai a razão social do empregador.
     */
    private String extractRazaoSocial(String text) {
        Pattern pattern = Pattern.compile("(?:Nome|Raz[aã]o Social)[:\\s]*([A-Z\\s]+(?:LTDA|S/A|ME|EPP|EIRELI)?)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Extrai o identificador da guia FGTS (ex: 0124040202313489-5).
     */
    private String extractIdentificador(String text) {
        Pattern pattern = Pattern.compile("(?:Identificador)[:\\s]*(\\d{16}-\\d)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Extrai a data de vencimento.
     */
    private LocalDate extractDataVencimento(String text) {
        Pattern pattern = Pattern.compile("(?:Vencimento|Data.*Vencimento)[:\\s]*(\\d{2}/\\d{2}/\\d{4})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String dateStr = matcher.group(1);
            try {
                return LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (Exception e) {
                log.warn("Erro ao parsear data de vencimento: {}", dateStr, e);
            }
        }
        return defaultDueDate(text);
    }

    /**
     * Extrai o valor total a recolher.
     */
    private BigDecimal extractValorTotal(String text) {
        Pattern pattern = Pattern.compile("(?:Valor.*Recolher|Total.*Recolher)[:\\s]*R?\\$?\\s*([0-9.,]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String valueStr = matcher.group(1).replaceAll("[^0-9,.]", "");
            try {
                return new BigDecimal(valueStr.replace(".", "").replace(",", "."));
            } catch (NumberFormatException e) {
                log.warn("Erro ao parsear valor total: {}", valueStr, e);
            }
        }
        return defaultTotalValue(text);
    }

    /**
     * Extrai o código de barras.
     */
    private String extractCodigoBarras(String text) {
        // Remove espaços e procura por 47/48 dígitos contíguos
        Pattern pattern = Pattern.compile("([0-9\\s]{44,50})");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String barcode = matcher.group(1).replaceAll("\\s", "");
            if (barcode.length() >= 44 && barcode.length() <= 48) {
                return barcode;
            }
        }
        return null;
    }

    /**
     * Extrai o código PIX copia e cola.
     */
    private String extractPixCopiaCola(String text) {
        Pattern pattern = Pattern.compile("(?:PIX.*Copia.*Cola|C[oó]pia.*Cola)[:\\s]*([A-Za-z0-9./-]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Extrai a composição do FGTS por competência.
     */
    private List<ExpenseFGTS.ComposicaoFGTS> extractComposicaoFgts(String text) {
        List<ExpenseFGTS.ComposicaoFGTS> composicoes = new ArrayList<>();

        // Regex para linhas de composição (ex: "09/2025 - 10 trabalhadores - Remuneração: 5.000,00 - FGTS: 400,00")
        Pattern pattern = Pattern.compile("(\\d{2}/\\d{4})\\s*-?\\s*(\\d+)\\s*(?:trabalhadores?)?.*?(?:Remunera[cç][aã]o|Sal[aá]rios?)[:\\s]*([0-9.,]+).*?FGTS[:\\s]*([0-9.,]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            ExpenseFGTS.ComposicaoFGTS composicao = new ExpenseFGTS.ComposicaoFGTS();
            composicao.setCompetencia(matcher.group(1));
            composicao.setQuantidadeTrabalhadores(Integer.parseInt(matcher.group(2)));
            composicao.setValorRemuneracao(parseValue(matcher.group(3)));
            composicao.setValorFgts(parseValue(matcher.group(4)));

            // Total é a soma de remuneração + FGTS (ou pode ser extraído separadamente)
            if (composicao.getValorRemuneracao() != null && composicao.getValorFgts() != null) {
                composicao.setValorTotal(composicao.getValorRemuneracao().add(composicao.getValorFgts()));
            }

            composicoes.add(composicao);
        }

        return composicoes.isEmpty() ? null : composicoes;
    }

    /**
     * Converte string de valor para BigDecimal.
     */
    private BigDecimal parseValue(String valueStr) {
        if (valueStr == null || valueStr.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(valueStr.replaceAll("[^0-9,.]", "").replace(".", "").replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
