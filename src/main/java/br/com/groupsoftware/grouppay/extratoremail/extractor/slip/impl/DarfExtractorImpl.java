package br.com.groupsoftware.grouppay.extratoremail.extractor.slip.impl;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Regex;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.ExpenseDARF;
import br.com.groupsoftware.grouppay.extratoremail.extractor.ExtractorTemplate;
import br.com.groupsoftware.grouppay.extratoremail.extractor.slip.DarfExtractor;
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
 * Implementação para extração de dados de documentos DARF
 * (Documento de Arrecadação de Receitas Federais).
 * <p>
 * Esta classe extende {@link ExtractorTemplate} e implementa {@link DarfExtractor},
 * fornecendo lógica para extrair informações como CNPJ do contribuinte, razão social,
 * período de apuração, data de vencimento, valor total, código de barras e composição de tributos.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2025
 */
@Slf4j
@Component
public class DarfExtractorImpl extends ExtractorTemplate implements DarfExtractor {

    public DarfExtractorImpl(RepositoryFacade facade) {
        super(facade);
    }

    @Override
    public Expense getExpense(Document document) {
        String text = document.getTextExtracted();
        Regex regex = getRegexByDocument(document);

        ExpenseDARF darf = new ExpenseDARF();

        // Extração de campos básicos usando regex se disponível
        if (regex != null) {
            darf.setCnpjCpfEmitente(extractByPattern(text, regex.getIssuerCNPJ()));
            darf.setDataEmissao(extractDate(text, regex.getIssueDate()));
            darf.setDataVencimento(extractDate(text, regex.getDueDate()));
            darf.setValorTotal(extractValueByPattern(text, regex.getTotalValue()));
            darf.setNumero(extractByPattern(text, regex.getNumber()));
        }

        // Fallback para extração com métodos default e regex específicos
        if (darf.getCnpjCpfEmitente() == null) {
            darf.setCnpjCpfEmitente(extractCnpjContribuinte(text));
        }

        if (darf.getDataVencimento() == null) {
            darf.setDataVencimento(extractDataVencimento(text));
        }

        if (darf.getValorTotal() == null) {
            darf.setValorTotal(extractValorTotal(text));
        }

        // Extração de campos específicos do DARF
        darf.setRazaoSocialContribuinte(extractRazaoSocial(text));
        darf.setPeriodoApuracao(extractPeriodoApuracao(text));
        darf.setNumeroDocumento(extractNumeroDocumento(text));
        darf.setNumeroRecibo(extractNumeroRecibo(text));
        darf.setCodigoBarras(extractCodigoBarras(text));
        darf.setPixCopiaCola(extractPixCopiaCola(text));
        darf.setComposicao(extractComposicaoTributos(text));

        // Emitente é o próprio contribuinte no DARF
        darf.setEmitente("Receita Federal do Brasil");

        return darf;
    }

    /**
     * Extrai o CNPJ/CPF do contribuinte do DARF.
     * Procura especificamente pelo CNPJ/CPF que aparece após "CNPJ" ou "CPF".
     */
    private String extractCnpjContribuinte(String text) {
        // Tenta extrair CNPJ (14 dígitos)
        Pattern pattern = Pattern.compile("(?:CNPJ|CPF)[:\\s]*([0-9./-]{14,20})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("[^0-9]", "");
        }

        // Fallback para primeiro CNPJ encontrado
        return defaultIssuerCNPJ(text);
    }

    /**
     * Extrai a razão social do contribuinte.
     */
    private String extractRazaoSocial(String text) {
        Pattern pattern = Pattern.compile("(?:Raz[aã]o Social|Nome)[:\\s]*([A-Z\\s]+(?:LTDA|S/A|ME|EPP|EIRELI)?)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Extrai o período de apuração (formato: Out/2025 ou 10/2025).
     */
    private String extractPeriodoApuracao(String text) {
        Pattern pattern = Pattern.compile("(?:Per[ií]odo.*Apura[cç][aã]o|PA)[:\\s]*([A-Za-z]{3}/\\d{4}|\\d{2}/\\d{4})", Pattern.CASE_INSENSITIVE);
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
     * Extrai o valor total do documento.
     */
    private BigDecimal extractValorTotal(String text) {
        Pattern pattern = Pattern.compile("(?:Valor Total|Total.*Documento)[:\\s]*R?\\$?\\s*([0-9.,]+)", Pattern.CASE_INSENSITIVE);
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
     * Extrai o número do documento DARF.
     */
    private String extractNumeroDocumento(String text) {
        Pattern pattern = Pattern.compile("(?:N[uú]mero.*Documento|Documento)[:\\s]*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Extrai o número do recibo.
     */
    private String extractNumeroRecibo(String text) {
        Pattern pattern = Pattern.compile("(?:N[uú]mero.*Recibo|Recibo)[:\\s]*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Extrai o código de barras (48 dígitos).
     */
    private String extractCodigoBarras(String text) {
        // Remove espaços e procura por 48 dígitos contíguos
        Pattern pattern = Pattern.compile("([0-9\\s]{48,60})");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String barcode = matcher.group(1).replaceAll("\\s", "");
            if (barcode.length() == 48) {
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
     * Extrai a composição de tributos do DARF.
     */
    private List<ExpenseDARF.TributoDARF> extractComposicaoTributos(String text) {
        List<ExpenseDARF.TributoDARF> tributos = new ArrayList<>();

        // Regex para linhas de tributos (ex: "1082 - IRPJ - Principal: 1.000,00 Multa: 50,00 Juros: 10,00 Total: 1.060,00")
        Pattern pattern = Pattern.compile("(\\d{4})\\s*-?\\s*([A-Z\\s]+?)\\s*-?\\s*(?:Principal|Valor)[:\\s]*([0-9.,]+)(?:.*Multa[:\\s]*([0-9.,]+))?(?:.*Juros[:\\s]*([0-9.,]+))?(?:.*Total[:\\s]*([0-9.,]+))?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            ExpenseDARF.TributoDARF tributo = new ExpenseDARF.TributoDARF();
            tributo.setCodigoReceita(matcher.group(1));
            tributo.setDenominacao(matcher.group(2).trim());
            tributo.setPrincipal(parseValue(matcher.group(3)));
            tributo.setMulta(parseValue(matcher.group(4)));
            tributo.setJuros(parseValue(matcher.group(5)));
            tributo.setTotal(parseValue(matcher.group(6)));

            tributos.add(tributo);
        }

        return tributos.isEmpty() ? null : tributos;
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
