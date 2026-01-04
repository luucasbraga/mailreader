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
        String text = document.getTextExtracted();
        Regex regex = getRegexByDocument(document);

        ExpenseBoleto despesaBoleto = new ExpenseBoleto();

        // Extração de campos básicos usando regex se disponível
        if (regex != null) {
            despesaBoleto.setDataEmissao(extractDate(text, regex.getIssueDate()));
            despesaBoleto.setDataVencimento(extractDate(text, regex.getDueDate()));
            despesaBoleto.setValorTotal(extractValueByPattern(text, regex.getTotalValue()));
            despesaBoleto.setNumero(extractByPattern(text, regex.getNumber()));
        }

        // Fallback para métodos específicos
        if (despesaBoleto.getDataVencimento() == null) {
            despesaBoleto.setDataVencimento(defaultDueDate(text));
        }

        if (despesaBoleto.getValorTotal() == null) {
            despesaBoleto.setValorTotal(defaultTotalValue(text));
        }

        // Extração específica do cedente/beneficiário (emitente do boleto)
        String cnpjCedente = extractCnpjCedente(text);
        if (cnpjCedente == null && regex != null) {
            cnpjCedente = extractByPattern(text, regex.getIssuerCNPJ());
        }
        despesaBoleto.setCnpjCpfEmitente(cnpjCedente);

        // Extração do nome do cedente
        String nomeCedente = extractNomeCedente(text);
        despesaBoleto.setCedente(nomeCedente);
        despesaBoleto.setEmitente(nomeCedente);

        // Extração do CNPJ do pagador/sacado (destinatário)
        despesaBoleto.setCnpjCpfDestinatario(extractCnpjPagador(text));

        // Extração de campos específicos do boleto
        despesaBoleto.setBancoEmissor(extractBanco(text));
        despesaBoleto.setLinhaDigitavel(extractLinhaDigitavel(text));
        despesaBoleto.setCodigoBarras(extractCodigoBarras(text));
        despesaBoleto.setNossoNumero(extractNossoNumero(text));
        despesaBoleto.setJuros(extractJuros(text));
        despesaBoleto.setMulta(extractMulta(text));
        despesaBoleto.setDescontos(extractDescontos(text));

        return despesaBoleto;
    }

    /**
     * Extrai o CNPJ/CPF do cedente/beneficiário (quem emitiu o boleto).
     * Procura especificamente após as palavras "Beneficiário" ou "Cedente".
     */
    private String extractCnpjCedente(String text) {
        // Tenta extrair CNPJ/CPF após "Beneficiário" ou "Cedente"
        Pattern pattern = Pattern.compile("(?:Benefici[aá]rio|Cedente)[:\\s]*.*?([0-9./-]{11,20})", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String cnpj = matcher.group(1).replaceAll("[^0-9]", "");
            // Valida se tem 11 (CPF) ou 14 (CNPJ) dígitos
            if (cnpj.length() == 11 || cnpj.length() == 14) {
                return cnpj;
            }
        }

        // Tenta extrair procurando por padrão de CNPJ/CPF próximo a Beneficiário
        Pattern patternLine = Pattern.compile("(?:Benefici[aá]rio|Cedente)[^\\n]{0,200}?([0-9]{2,3}[.\\-]?[0-9]{3}[.\\-]?[0-9]{3}[/\\-]?[0-9]{4}[\\-]?[0-9]{2}|[0-9]{3}[.\\-]?[0-9]{3}[.\\-]?[0-9]{3}[\\-]?[0-9]{2})", Pattern.CASE_INSENSITIVE);
        Matcher matcherLine = patternLine.matcher(text);
        if (matcherLine.find()) {
            return matcherLine.group(1).replaceAll("[^0-9]", "");
        }

        return null;
    }

    /**
     * Extrai o nome do cedente/beneficiário.
     */
    private String extractNomeCedente(String text) {
        Pattern pattern = Pattern.compile("(?:Benefici[aá]rio|Cedente)[:\\s]*([A-Z][A-Za-z\\s]+?)(?:CNPJ|CPF|\\d{2,3}\\.)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim().replaceAll("\\s{2,}", " ");
        }
        return null;
    }

    /**
     * Extrai o CNPJ/CPF do pagador/sacado.
     */
    private String extractCnpjPagador(String text) {
        Pattern pattern = Pattern.compile("(?:Pagador|Sacado)[:\\s]*.*?([0-9./-]{11,20})", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String cnpj = matcher.group(1).replaceAll("[^0-9]", "");
            if (cnpj.length() == 11 || cnpj.length() == 14) {
                return cnpj;
            }
        }
        return null;
    }

    /**
     * Extrai a linha digitável do boleto.
     * Formato padrão: XXXXX.XXXXX XXXXX.XXXXXX XXXXX.XXXXXX X XXXXXXXXXXXXXX
     */
    private String extractLinhaDigitavel(String text) {
        // Padrão com pontos e espaços (formato padrão)
        Pattern pattern = Pattern.compile("(\\d{5}\\.\\d{5}\\s+\\d{5}\\.\\d{6}\\s+\\d{5}\\.\\d{6}\\s+\\d{1}\\s+\\d{14})");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Padrão sem pontos mas com espaços
        Pattern patternNoDots = Pattern.compile("(\\d{5}\\s?\\d{5}\\s+\\d{5}\\s?\\d{6}\\s+\\d{5}\\s?\\d{6}\\s+\\d{1}\\s+\\d{14})");
        Matcher matcherNoDots = patternNoDots.matcher(text);
        if (matcherNoDots.find()) {
            return matcherNoDots.group(1);
        }

        // Padrão de 47 dígitos contíguos
        Pattern pattern47 = Pattern.compile("(\\d{47})");
        Matcher matcher47 = pattern47.matcher(text);
        if (matcher47.find()) {
            String digits = matcher47.group(1);
            // Formata: 5 + 5 + 5 + 6 + 5 + 6 + 1 + 14
            return String.format("%s.%s %s.%s %s.%s %s %s",
                digits.substring(0, 5), digits.substring(5, 10),
                digits.substring(10, 15), digits.substring(15, 21),
                digits.substring(21, 26), digits.substring(26, 32),
                digits.substring(32, 33), digits.substring(33, 47));
        }

        return null;
    }

    /**
     * Extrai o código de barras do boleto (44 ou 47 dígitos).
     */
    private String extractCodigoBarras(String text) {
        Pattern pattern = Pattern.compile("([0-9\\s]{44,50})");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String barcode = matcher.group(1).replaceAll("\\s", "");
            if (barcode.length() == 44 || barcode.length() == 47) {
                return barcode;
            }
        }
        return null;
    }

    /**
     * Extrai o banco emissor.
     * Tenta identificar pelo código do banco (104=CAIXA, 237=BRADESCO, etc.).
     */
    private String extractBanco(String text) {
        // Mapeia códigos de banco conhecidos
        if (text.contains("104") || text.toLowerCase().contains("caixa")) {
            return "CAIXA";
        } else if (text.contains("237") || text.toLowerCase().contains("bradesco")) {
            return "BRADESCO";
        } else if (text.contains("341") || text.toLowerCase().contains("itau") || text.toLowerCase().contains("itaú")) {
            return "ITAU";
        } else if (text.contains("001") || text.toLowerCase().contains("banco do brasil")) {
            return "BANCO DO BRASIL";
        } else if (text.contains("033") || text.toLowerCase().contains("santander")) {
            return "SANTANDER";
        }

        // Tenta extrair nome do banco
        Pattern pattern = Pattern.compile("(?:Banco|Bank)[:\\s]*([A-Z\\s]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    /**
     * Extrai o nosso número.
     */
    private String extractNossoNumero(String text) {
        Pattern pattern = Pattern.compile("(?:Nosso.*N[uú]mero)[:\\s]*([0-9\\-]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Extrai o valor dos juros.
     */
    private java.math.BigDecimal extractJuros(String text) {
        Pattern pattern = Pattern.compile("(?:Juros)[:\\s]*R?\\$?\\s*([0-9.,]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return extractValueByPattern(text, pattern.pattern());
        }
        return null;
    }

    /**
     * Extrai o valor da multa.
     */
    private java.math.BigDecimal extractMulta(String text) {
        Pattern pattern = Pattern.compile("(?:Multa)[:\\s]*R?\\$?\\s*([0-9.,]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return extractValueByPattern(text, pattern.pattern());
        }
        return null;
    }

    /**
     * Extrai o valor dos descontos.
     */
    private java.math.BigDecimal extractDescontos(String text) {
        Pattern pattern = Pattern.compile("(?:Desconto)[:\\s]*R?\\$?\\s*([0-9.,]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return extractValueByPattern(text, pattern.pattern());
        }
        return null;
    }
}
