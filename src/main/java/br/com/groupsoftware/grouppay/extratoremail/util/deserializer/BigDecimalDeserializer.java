package br.com.groupsoftware.grouppay.extratoremail.util.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * Utilitário para manipulação e desserialização de valores em formatos variados.
 * <p>
 * Esta classe auxilia na desserialização de strings de data para objetos {@link BigDecimal},
 * suportando múltiplos formatos de valores, como ISO, brasileiro e americano.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
public class BigDecimalDeserializer extends JsonDeserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value != null) {
            value = value.replace(",", ".");
        }
        return value != null ? new BigDecimal(value) : null;
    }
}

