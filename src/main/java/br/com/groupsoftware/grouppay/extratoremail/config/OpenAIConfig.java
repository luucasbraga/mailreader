package br.com.groupsoftware.grouppay.extratoremail.config;

import br.com.groupsoftware.grouppay.extratoremail.domain.enums.ExpenseType;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Classe de configuração para integração com a API da OpenAI.
 *
 * <p>
 * Esta classe centraliza todas as configurações necessárias para se comunicar com a API da OpenAI,
 * como URL, modelo de IA utilizado, chave de autenticação e o mapeamento de tipos de despesas para
 * suas respectivas classes de modelo.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Configuration
@ConfigurationProperties(prefix = "openai.api")
@Getter
@Setter
public class OpenAIConfig {

    private String url;
    private String model;
    private String key;

    private final Map<ExpenseType, Class<?>> expenseTypeMap = Map.of(
            ExpenseType.NFE, ExpenseNF.class,
            ExpenseType.BOLETO, ExpenseBoleto.class,
            ExpenseType.FATURA, ExpenseFatura.class,
            ExpenseType.NFSE, ExpenseNFS.class,
            ExpenseType.NF3E, ExpenseNF3.class,
            ExpenseType.NFCE, ExpenseNFC.class,
            ExpenseType.CTE, ExpenseCT.class
    );
}

