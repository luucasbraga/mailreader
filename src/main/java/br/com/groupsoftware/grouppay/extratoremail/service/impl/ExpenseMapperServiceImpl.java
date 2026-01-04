package br.com.groupsoftware.grouppay.extratoremail.service.impl;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.*;
import br.com.groupsoftware.grouppay.extratoremail.service.ExpenseMapperService;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Implementação do serviço de mapeamento de despesas.
 * <p>
 * Esta classe é responsável por converter o JSON armazenado em um objeto
 * {@link Expense} específico com base no tipo da despesa definido no documento.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @see Document
 * @see Expense
 * @see ExpenseMapperService
 * @see ObjectMapper
 * @since 2024
 */
@Slf4j
@Component
@RequiredArgsConstructor
class ExpenseMapperServiceImpl implements ExpenseMapperService {
    private final ObjectMapper objectMapper;

    public Expense getExpense(Document document) {
        try {
            Expense expense;

            switch (document.getExpenseType()) {
                case NFE:
                    expense = objectMapper.readValue(document.getExpenseJson(), ExpenseNF.class);
                    break;
                case BOLETO:
                    expense = objectMapper.readValue(document.getExpenseJson(), ExpenseBoleto.class);
                    break;
                case FATURA:
                    expense = objectMapper.readValue(document.getExpenseJson(), ExpenseFatura.class);
                    break;
                case NFSE:
                    expense = objectMapper.readValue(document.getExpenseJson(), ExpenseNFS.class);
                    break;
                case NF3E:
                    expense = objectMapper.readValue(document.getExpenseJson(), ExpenseNF3.class);
                    break;
                case NFCE:
                    expense = objectMapper.readValue(document.getExpenseJson(), ExpenseNFC.class);
                    break;
                case CTE:
                    expense = objectMapper.readValue(document.getExpenseJson(), ExpenseCT.class);
                    break;
                default:
                    log.error("Tipo de despesa não reconhecido: {}", document.getExpenseType());
                    return null;
            }
            return expense;

        } catch (JsonParseException e) {
            log.error("Erro ao parsear o JSON: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Erro geral: {}", e.getMessage(), e);
        }
        return null;
    }
}
