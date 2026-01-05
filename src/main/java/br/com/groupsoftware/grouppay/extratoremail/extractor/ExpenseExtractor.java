package br.com.groupsoftware.grouppay.extratoremail.extractor;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ‘Interface’ para extração de despesas a partir de um documento fiscal.
 * <p>
 * Esta ‘interface’ define o contrato para implementar a lógica de extração de dados
 * que resultam na criação de um objeto {@link Expense} com as informações contidas
 * no {@link Document}. As implementações concretas devem processar o conteúdo
 * do documento e retornar um objeto Expense devidamente populado.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2025
 */
public interface ExpenseExtractor {
    Expense getExpense(Document document);

    default String extractByPattern(String text, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return null;
        }
        Pattern compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = compiledPattern.matcher(text);
        if (matcher.find()) {
            if (matcher.groupCount() >= 1 && matcher.group(1) != null) {
                return matcher.group(1).trim();
            }
        }
        return null;
    }

    default LocalDate extractDate(String text, String pattern) {
        String dateStr = extractByPattern(text, pattern);
        if (dateStr != null) {
            return LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        return null;
    }

    default BigDecimal extractValueByPattern(String text, String pattern) {
        String valueStr = extractByPattern(text, pattern);
        if (valueStr != null && !valueStr.isEmpty()) {
            valueStr = valueStr.replaceAll("[^0-9,.]", "");
            try {
                return new BigDecimal(valueStr.replace(".", "").replace(",", "."));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    default String extractIssuerName(String text, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return null;
        }
        Pattern compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = compiledPattern.matcher(text);
        if (matcher.find() && matcher.groupCount() >= 1 && matcher.group(1) != null) {
            return matcher.group(1).trim().replaceAll("\\s{2,}", " ");
        }
        return null;
    }


    default LocalDate defaultDueDate(String text) {
        List<LocalDate> datas = new ArrayList<>();
        Pattern dataPattern = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})");
        Matcher dataMatcher = dataPattern.matcher(text);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        while (dataMatcher.find()) {
            String dataStr = dataMatcher.group(1);
            try {
                LocalDate data = LocalDate.parse(dataStr, formatter);
                datas.add(data);
            } catch (Exception ignored) {
            }
        }
        if (!datas.isEmpty()) {
            Collections.sort(datas);
            return datas.getFirst();
        }
        return null;
    }

    default BigDecimal defaultTotalValue(String text) {
        List<BigDecimal> valores = new ArrayList<>();
        Pattern valorPattern = Pattern.compile("([0-9]{1,3}(?:\\.[0-9]{3})*,[0-9]{2})");
        Matcher valorMatcher = valorPattern.matcher(text);
        while (valorMatcher.find()) {
            String valorStr = valorMatcher.group(1).replaceAll("\\.", "").replace(",", ".");
            try {
                BigDecimal valor = new BigDecimal(valorStr);
                valores.add(valor);
            } catch (NumberFormatException ignored) {}
        }
        if (!valores.isEmpty()) {
            return valores.stream().max(BigDecimal::compareTo).orElse(null);
        }
        return null;
    }

    default String defaultIssuerCNPJ(String text) {
        Pattern pattern = Pattern.compile("(\\d{2}[.\\-]?\\d{3}[.\\-]?\\d{3}[/\\-]?\\d{4}[\\-/]?\\d{2})");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("\\D", "");
        }
        return null;
    }

    /**
     * Extrai a chave de acesso de documentos fiscais eletrônicos (44 dígitos).
     * A chave pode estar com ou sem espaços no documento.
     *
     * @param text Texto do documento
     * @return Chave de acesso com 44 dígitos, ou null se não encontrada
     */
    default String extractChaveAcesso(String text) {
        // Tenta encontrar após os termos "Chave de Acesso" ou "CHAVE"
        Pattern pattern = Pattern.compile("(?:Chave\\s+de\\s+Acesso|CHAVE)[:\\s]*([0-9\\s]{44,60})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String chave = matcher.group(1).replaceAll("\\s", "");
            if (chave.length() == 44) {
                return chave;
            }
        }

        // Procura por sequência de 44 dígitos com ou sem espaços
        Pattern pattern44 = Pattern.compile("([0-9\\s]{50,70})");
        Matcher matcher44 = pattern44.matcher(text);
        while (matcher44.find()) {
            String candidate = matcher44.group(1).replaceAll("\\s", "");
            if (candidate.length() == 44) {
                // Valida se parece uma chave de acesso (começa com código UF válido)
                String uf = candidate.substring(0, 2);
                int ufCode = Integer.parseInt(uf);
                if (ufCode >= 11 && ufCode <= 53) { // Códigos UF válidos
                    return candidate;
                }
            }
        }

        return null;
    }
}
