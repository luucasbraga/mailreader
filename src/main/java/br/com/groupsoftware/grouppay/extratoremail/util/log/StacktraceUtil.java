package br.com.groupsoftware.grouppay.extratoremail.util.log;

import lombok.experimental.UtilityClass;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Utilitário para manipulação de stack traces.
 * <p>
 * Esta classe fornece métodos para capturar o stack trace completo de uma exceção e convertê-lo para uma lista de strings.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@UtilityClass
public class StacktraceUtil {

    public List<String> getStacktraceAsList(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        String fullStacktrace = stringWriter.toString();
        String[] lines = fullStacktrace.split(System.lineSeparator());
        return new ArrayList<>(Arrays.asList(lines));
    }
}

