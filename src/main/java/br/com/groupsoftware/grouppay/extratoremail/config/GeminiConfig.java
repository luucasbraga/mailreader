package br.com.groupsoftware.grouppay.extratoremail.config;

import br.com.groupsoftware.grouppay.extratoremail.domain.enums.ExpenseType;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Classe de configuração para integração com a API Gemini.
 *
 * <p>
 * Esta classe centraliza todas as configurações necessárias para se comunicar com a API Gemini,
 * como URL, modelo de IA utilizado, chave de autenticação e o mapeamento de tipos de despesas para
 * suas respectivas classes de modelo.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2025
 */
@Configuration
@ConfigurationProperties(prefix = "gemini.api")
@Getter
@Setter
public class GeminiConfig {

    private String apiKey;
    private String baseUrl;
    private String modelName;
    private String generateContentEndpoint;

    private Map<ExpenseType, Class<?>> expenseTypeMap = Map.ofEntries(
            Map.entry(ExpenseType.NFE, ExpenseNF.class),
            Map.entry(ExpenseType.BOLETO, ExpenseBoleto.class),
            Map.entry(ExpenseType.FATURA, ExpenseFatura.class),
            Map.entry(ExpenseType.NFSE, ExpenseNFS.class),
            Map.entry(ExpenseType.NF3E, ExpenseNF3.class),
            Map.entry(ExpenseType.NFCE, ExpenseNFC.class),
            Map.entry(ExpenseType.CTE, ExpenseCT.class),
            Map.entry(ExpenseType.DARF, ExpenseDARF.class),
            Map.entry(ExpenseType.FGTS, ExpenseFGTS.class),
            Map.entry(ExpenseType.GPS, ExpenseGPS.class)
    );

    public String getUrl() {
        return baseUrl + "/models/" + modelName + ":" + generateContentEndpoint + "?key=" + apiKey;
    }
}