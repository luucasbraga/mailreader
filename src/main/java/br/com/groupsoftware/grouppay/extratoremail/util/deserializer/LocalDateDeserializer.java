package br.com.groupsoftware.grouppay.extratoremail.util.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/**
 * Utilitário para manipulação e desserialização de datas em formatos variados.
 * <p>
 * Esta classe auxilia na desserialização de strings de data para objetos {@link LocalDate},
 * suportando múltiplos formatos de data, como ISO, brasileiro e americano.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */

public class LocalDateDeserializer extends JsonDeserializer<LocalDate> {

    // Lista de formatos suportados
    private static final List<DateTimeFormatter> FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),   // Formato ISO
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),   // Formato brasileiro
            DateTimeFormatter.ofPattern("MM/dd/yyyy")    // Formato americano
    );


    public static LocalDate parseData(String dataStr) {
        if (dataStr == null || dataStr.isBlank() || dataStr.equals("0000-00-00")) {
            return null;
        }

        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDate.parse(dataStr, formatter);
            } catch (DateTimeParseException ignored) {
                // Ignorar e continuar tentando com outros formatos
            }
        }
        return null;
    }

    @Override
    public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String date = parser.getText().trim();
        return parseData(date);
    }
}

