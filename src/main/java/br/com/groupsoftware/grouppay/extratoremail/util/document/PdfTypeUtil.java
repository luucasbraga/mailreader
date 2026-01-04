package br.com.groupsoftware.grouppay.extratoremail.util.document;

import br.com.groupsoftware.grouppay.extratoremail.domain.enums.ExpenseType;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitário para identificação do tipo de documento com base em padrões textuais.
 * <p>
 * Esta classe contém métodos que permitem identificar o tipo de documento a partir de seu conteúdo textual,
 * como Notas Fiscais Eletrônicas (NFE), Notas Fiscais de Serviço (NFSE), entre outros. A identificação é feita
 * através de expressões regulares que procuram por termos específicos relacionados a cada tipo de documento.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Slf4j
@UtilityClass
public class PdfTypeUtil {

    public ExpenseType identificarTipoPdf(String text) {
        if (containsNFSETerm(text)) {
            return ExpenseType.NFSE;
        } else if (containsNFCETerm(text)) {
            return ExpenseType.NFCE;
        } else if (containsNF3ETerm(text)) {
            return ExpenseType.NF3E;
        } else if (containsNFETerm(text)) {
            return ExpenseType.NFE;
        } else if (containsFaturaTerm(text)) {
            return ExpenseType.FATURA;
        } else if (containsBoletoTerm(text)) {
            return ExpenseType.BOLETO;
        } else if (containsCTETerm(text)) {
            return ExpenseType.CTE;
        }
        return ExpenseType.OUTRO;
    }

    private boolean containsNFETerm(String text) {
        return containsPattern(text, "\\bNF[\\s-]?e\\b");
    }

    private boolean containsNFSETerm(String text) {
        return containsPattern(text, "\\bNFS([\\s-]?e)?\\b");
    }

    private boolean containsNFCETerm(String text) {
        return containsPattern(text, "\\bNFC([\\s-]?e)?\\b");
    }

    private boolean containsNF3ETerm(String text) {
        return containsPattern(text, "\\bNF3([\\s-]?e)?\\b")
                || containsPattern(text, "(?i)Nota Fiscal\\s*-\\s*Conta de Energia El[eé]ctrica")
                || containsPattern(text, "(?i)Leitura\\s+Anterior");
    }

    private boolean containsCTETerm(String text) {
        return containsPattern(text, "\\bCT([\\s-]?e)?\\b");
    }

    private boolean containsBoletoTerm(String text) {
        return containsPattern(text, "\\b("
                + "boleto(s)? (banc[aá]rio(s)?)?"
                + "|nosso n[uú]mero"
                + "|boleto(s)? de pagamento"
                + "|cobran[cç]a(s)?"
                + "|linha digit[aá]vel"
                + "|guia do fgts"
                + "|documento de arrecada[cç][aã]o de receitas federais"
                + ")\\b");
    }

    private boolean containsFaturaTerm(String text) {
        return containsPattern(text, "\\bfatura\\b");
    }

    private boolean containsPattern(String text, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }
}
