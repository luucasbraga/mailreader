package br.com.groupsoftware.grouppay.extratoremail.service;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.model.Expense;

/**
 * Interface para o serviço de integração com o Amazon S3.
 * <p>
 * Define os métodos necessários para operações com o S3, como o upload
 * de arquivos para um bucket específico e o retorno do eTag para confirmação de sucesso.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
public interface ExpenseMapperService {
    Expense getExpense(Document document);
}
