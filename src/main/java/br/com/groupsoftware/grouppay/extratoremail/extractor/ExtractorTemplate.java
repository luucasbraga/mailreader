package br.com.groupsoftware.grouppay.extratoremail.extractor;

import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Document;
import br.com.groupsoftware.grouppay.extratoremail.domain.entity.Regex;
import br.com.groupsoftware.grouppay.extratoremail.domain.enums.ExpenseType;
import br.com.groupsoftware.grouppay.extratoremail.repository.RepositoryFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Classe abstrata que serve como template para a extração de dados utilizando padrões regex,
 * baseando-se no tipo de despesa e no subtipo do documento.
 * <p>
 * Esta classe fornece um método para recuperar um objeto {@link Regex} do repositório subjacente.
 * Se um regex para o subtipo especificado não for encontrado, será utilizado o regex padrão para o tipo de despesa.
 * As classes que estendem este template devem implementar a lógica específica de extração.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2025
 */
@Slf4j
@Component
@RequiredArgsConstructor
public abstract class ExtractorTemplate {

    private final RepositoryFacade facade;

    protected Regex getRegexByDocumentTypeAndRegion(ExpenseType expenseType, String ibgeCode) {
        Regex regex = facade.regex.findByExpenseTypeAndIbgeCode(expenseType, ibgeCode);
        if (regex == null) {
            regex = facade.regex.findByExpenseTypeAndIbgeCode(expenseType, "DEFAULT");
        }
        return regex;
    }

    protected Regex getRegexByDocument(Document document) {
        return getRegexByDocumentTypeAndRegion(document.getExpenseType(), String.valueOf(document.getCompany().getCity().getIbgeCode()));
    }
}
