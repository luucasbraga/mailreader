package br.com.groupsoftware.grouppay.extratoremail.util.brazil;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utilitário para formatação, validação e manipulação de CPFs e CNPJs.
 * <p>
 * Esta classe fornece métodos para:
 * <ul>
 *     <li>Formatar uma sequência de caracteres numéricos como CPF ou CNPJ, aplicando a máscara apropriada;</li>
 *     <li>Validar se um CPF ou CNPJ é válido, verificando os dígitos verificadores;</li>
 *     <li>Extrair somente os dígitos numéricos de uma string;</li>
 *     <li>Converter uma sequência numérica em lista de inteiros e vice-versa;</li>
 *     <li>Realizar o cálculo do dígito verificador utilizando o método módulo 11.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Exemplo de uso:
 * <pre>
 *     String cpfFormatado = CpfCnpjUtil.format("12345678909");
 *     boolean cpfValido = CpfCnpjUtil.isCpfValid("12345678909");
 * </pre>
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2025
 */
@UtilityClass
@Slf4j
public class CpfCnpjUtil {

    /**
     * Formata um número de CPF ou CNPJ.
     * <p>
     * Se a sequência de dígitos tiver 11 dígitos, será formatada como CPF (XXX.XXX.XXX-XX).
     * Se tiver 14 dígitos, será formatada como CNPJ (XX.XXX.XXX/XXXX-XX).
     * Caso contrário, retorna a sequência de dígitos extraída.
     * </p>
     *
     * @param value Valor contendo o CPF ou CNPJ possivelmente com formatação irregular.
     * @return A string formatada de acordo com o padrão do CPF ou CNPJ.
     */
    public static String format(final String value) {
        final var val = extractNumbers(value);
        if (val.length() == 11) {
            return val.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        } else if (val.length() == 14) {
            return val.replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
        }
        return val;
    }

    /**
     * Verifica se um CPF ou CNPJ é válido.
     * <p>
     * O método retorna {@code true} se o valor informado for um CPF válido ou um CNPJ válido,
     * utilizando os respectivos métodos de validação.
     * </p>
     *
     * @param cnpjCpf String contendo o CPF ou CNPJ.
     * @return {@code true} se o CPF ou CNPJ for válido; {@code false} caso contrário.
     */
    public static boolean isCnpjCpfValid(final String cnpjCpf) {
        return isCpfValid(cnpjCpf) || isCnpjValid(cnpjCpf);
    }

    /**
     * Verifica a validade de um CPF.
     * <p>
     * O método extrai os dígitos numéricos do CPF, garante que não sejam todos iguais e utiliza
     * o cálculo dos dígitos verificadores para confirmar a validade.
     * </p>
     *
     * @param cpf String contendo o CPF.
     * @return {@code true} se o CPF for válido; {@code false} caso contrário.
     */
    public static boolean isCpfValid(final String cpf) {
        final List<Integer> digits = extractNumbersToList(cpf);
        if (digits.size() == 11 && digits.stream().distinct().count() > 1) {
            return getCpfValid(digits.subList(0, 9)).equals(extractNumbers(cpf));
        }
        return false;
    }

    /**
     * Verifica a validade de um CNPJ.
     * <p>
     * O método extrai os dígitos numéricos do CNPJ, garante que não sejam todos iguais e utiliza
     * o cálculo dos dígitos verificadores para confirmar a validade.
     * </p>
     *
     * @param cnpj String contendo o CNPJ.
     * @return {@code true} se o CNPJ for válido; {@code false} caso contrário.
     */
    public static boolean isCnpjValid(final String cnpj) {
        final List<Integer> digits = extractNumbersToList(cnpj);
        if (digits.size() == 14 && digits.stream().distinct().count() > 1) {
            return getCnpjValid(digits.subList(0, 12)).equals(extractNumbers(cnpj));
        }
        return false;
    }

    /**
     * Calcula e retorna o CPF completo com os dígitos verificadores.
     * <p>
     * A partir dos 9 primeiros dígitos, este método calcula os dois dígitos verificadores
     * utilizando o algoritmo módulo 11 e adiciona-os à lista original, retornando o CPF completo.
     * </p>
     *
     * @param digits Lista de inteiros representando os 9 primeiros dígitos do CPF.
     * @return O CPF completo em formato de string com os dígitos verificadores calculados.
     */
    private static String getCpfValid(final List<Integer> digits) {
        digits.add(mod11(digits, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        digits.add(mod11(digits, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        return listToString(digits);
    }

    /**
     * Calcula e retorna o CNPJ completo com os dígitos verificadores.
     * <p>
     * A partir dos 12 primeiros dígitos, este método calcula os dois dígitos verificadores
     * utilizando o algoritmo módulo 11 e adiciona-os à lista original, retornando o CNPJ completo.
     * </p>
     *
     * @param digits Lista de inteiros representando os 12 primeiros dígitos do CNPJ.
     * @return O CNPJ completo em formato de string com os dígitos verificadores calculados.
     */
    private static String getCnpjValid(final List<Integer> digits) {
        digits.add(mod11(digits, 6, 7, 8, 9, 2, 3, 4, 5, 6, 7, 8, 9));
        digits.add(mod11(digits, 5, 6, 7, 8, 9, 2, 3, 4, 5, 6, 7, 8, 9));
        return listToString(digits);
    }

    /**
     * Realiza o cálculo do dígito verificador utilizando o método módulo 11.
     * <p>
     * Multiplica cada dígito da lista pelos multiplicadores correspondentes, soma os resultados e
     * calcula o resto da divisão por 11. Se o resto for maior que 9, retorna 0; caso contrário, retorna o resto.
     * </p>
     *
     * @param digits     Lista de inteiros a serem utilizados no cálculo.
     * @param multipliers Vetor de multiplicadores a serem aplicados.
     * @return O dígito verificador calculado.
     */
    private static int mod11(final List<Integer> digits, final int... multipliers) {
        final var i = new AtomicInteger(0);
        final var rest = digits.stream().reduce(0, (p, e) -> p + e * multipliers[i.getAndIncrement()]) % 11;
        return rest > 9 ? 0 : rest;
    }

    /**
     * Extrai e retorna apenas os dígitos numéricos de uma string.
     * <p>
     * Caso a string seja nula, retorna uma string vazia.
     * </p>
     *
     * @param s A string de onde os dígitos serão extraídos.
     * @return Uma string contendo apenas os dígitos numéricos.
     */
    private static String extractNumbers(final String s) {
        return Objects.nonNull(s) ? s.replaceAll("\\D+", "") : "";
    }

    /**
     * Converte uma string em uma lista de inteiros, contendo apenas os dígitos numéricos.
     * <p>
     * Este método extrai os dígitos da string e os converte para uma lista de inteiros.
     * </p>
     *
     * @param value A string contendo os dígitos.
     * @return Uma lista de inteiros representando os dígitos extraídos.
     */
    private static List<Integer> extractNumbersToList(final String value) {
        final var digits = new ArrayList<Integer>();
        for (char item : extractNumbers(value).toCharArray()) {
            digits.add(Integer.parseInt(String.valueOf(item)));
        }
        return digits;
    }

    /**
     * Converte uma lista de inteiros em uma string concatenada.
     * <p>
     * Cada inteiro da lista é convertido para string e todos são concatenados para formar uma única string.
     * </p>
     *
     * @param list A lista de inteiros a ser convertida.
     * @return Uma string representando a concatenação dos inteiros da lista.
     */
    private static String listToString(final List<Integer> list) {
        return list.stream().map(Object::toString).reduce("", (p, e) -> p.concat(e));
    }
}
