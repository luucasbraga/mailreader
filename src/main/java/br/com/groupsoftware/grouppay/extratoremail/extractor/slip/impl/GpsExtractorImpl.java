package br.com.groupsoftware.grouppay.extratoremail.extractor.slip.impl;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Regex;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.ExpenseGPS;
import br.com.groupsoftware.grouppay.extratoremail.extractor.ExtractorTemplate;
import br.com.groupsoftware.grouppay.extratoremail.extractor.slip.GpsExtractor;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementação para extração de dados de GPS (Guia da Previdência Social).
 * <p>
 * Esta classe estende {@link ExtractorTemplate} e implementa {@link GpsExtractor},
 * fornecendo lógica para extrair informações como CNPJ do contribuinte, razão social,
 * código de pagamento, competência, data de vencimento, valores de INSS e código de barras.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2025
 */
@Slf4j
@Component
public class GpsExtractorImpl extends ExtractorTemplate implements GpsExtractor {

    public GpsExtractorImpl(RepositoryFacade facade) {
        super(facade);
    }

    @Override
    public Expense getExpense(Document document) {
        String text = document.getTextExtracted();
        Regex regex = getRegexByDocument(document);

        ExpenseGPS gps = new ExpenseGPS();

        // Extração de campos básicos usando regex se disponível
        if (regex != null) {
            gps.setCnpjCpfEmitente(extractByPattern(text, regex.getIssuerCNPJ()));
            gps.setDataEmissao(extractDate(text, regex.getIssueDate()));
            gps.setDataVencimento(extractDate(text, regex.getDueDate()));
            gps.setValorTotal(extractValueByPattern(text, regex.getTotalValue()));
            gps.setNumero(extractByPattern(text, regex.getNumber()));
        }

        // Fallback para extração com métodos default e regex específicos
        if (gps.getCnpjCpfEmitente() == null) {
            gps.setCnpjCpfEmitente(extractCnpjContribuinte(text));
        }

        if (gps.getDataVencimento() == null) {
            gps.setDataVencimento(extractDataVencimento(text));
        }

        if (gps.getValorTotal() == null) {
            gps.setValorTotal(extractValorTotal(text));
        }

        // Extração de campos específicos do GPS
        gps.setRazaoSocialContribuinte(extractRazaoSocial(text));
        gps.setCodigoPagamento(extractCodigoPagamento(text));
        gps.setCompetencia(extractCompetencia(text));
        gps.setIdentificador(extractIdentificador(text));
        gps.setCodigoBarras(extractCodigoBarras(text));
        gps.setPixCopiaCola(extractPixCopiaCola(text));
        gps.setValorINSS(extractValorINSS(text));
        gps.setValorOutrasEntidades(extractValorOutrasEntidades(text));
        gps.setAtualizacaoMonetaria(extractAtualizacaoMonetaria(text));
        gps.setJuros(extractJuros(text));
        gps.setMulta(extractMulta(text));

        // Emitente é a Receita Federal/INSS
        gps.setEmitente("Receita Federal do Brasil - INSS");

        return gps;
    }

    /**
     * Extrai o CNPJ/CPF do contribuinte.
     */
    private String extractCnpjContribuinte(String text) {
        Pattern pattern = Pattern.compile("(?:CNPJ/CEI/NIT|CNPJ)[:\\s]*([0-9./-]{11,20})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("[^0-9]", "");
        }
        return defaultIssuerCNPJ(text);
    }

    /**
     * Extrai a razão social do contribuinte.
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
     * Extrai o código de pagamento (ex: 2100, 2208).
     */
    private String extractCodigoPagamento(String text) {
        Pattern pattern = Pattern.compile("(?:C[oó]digo.*Pagamento)[:\\s]*(\\d{4})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Extrai a competência (formato: 10/2025).
     */
    private String extractCompetencia(String text) {
        Pattern pattern = Pattern.compile("(?:Compet[eê]ncia)[:\\s]*(\\d{2}/\\d{4})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Extrai o identificador (CEI ou NIT).
     */
    private String extractIdentificador(String text) {
        Pattern pattern = Pattern.compile("(?:CEI|NIT)[:\\s]*(\\d{11,14})", Pattern.CASE_INSENSITIVE);
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
     * Extrai o valor total.
     */
    private BigDecimal extractValorTotal(String text) {
        Pattern pattern = Pattern.compile("(?:Total.*Pagar|Valor Total)[:\\s]*R?\\$?\\s*([0-9.,]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return parseValue(matcher.group(1));
        }
        return defaultTotalValue(text);
    }

    /**
     * Extrai o código de barras.
     */
    private String extractCodigoBarras(String text) {
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
     * Extrai o valor do INSS.
     */
    private BigDecimal extractValorINSS(String text) {
        Pattern pattern = Pattern.compile("(?:INSS|Previd[eê]ncia)[:\\s]*R?\\$?\\s*([0-9.,]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return parseValue(matcher.group(1));
        }
        return null;
    }

    /**
     * Extrai o valor de outras entidades.
     */
    private BigDecimal extractValorOutrasEntidades(String text) {
        Pattern pattern = Pattern.compile("(?:Outras Entidades)[:\\s]*R?\\$?\\s*([0-9.,]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return parseValue(matcher.group(1));
        }
        return null;
    }

    /**
     * Extrai a atualização monetária.
     */
    private BigDecimal extractAtualizacaoMonetaria(String text) {
        Pattern pattern = Pattern.compile("(?:Atualiza[cç][aã]o.*Monet[aá]ria)[:\\s]*R?\\$?\\s*([0-9.,]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return parseValue(matcher.group(1));
        }
        return null;
    }

    /**
     * Extrai os juros.
     */
    private BigDecimal extractJuros(String text) {
        Pattern pattern = Pattern.compile("(?:Juros)[:\\s]*R?\\$?\\s*([0-9.,]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return parseValue(matcher.group(1));
        }
        return null;
    }

    /**
     * Extrai a multa.
     */
    private BigDecimal extractMulta(String text) {
        Pattern pattern = Pattern.compile("(?:Multa)[:\\s]*R?\\$?\\s*([0-9.,]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return parseValue(matcher.group(1));
        }
        return null;
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
