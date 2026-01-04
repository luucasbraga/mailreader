package br.com.groupsoftware.grouppay.extratoremail.config;

import br.com.groupsoftware.grouppay.extratoremail.util.deserializer.BigDecimalDeserializer;
import br.com.groupsoftware.grouppay.extratoremail.util.deserializer.LocalDateDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Configuração personalizada do Jackson para integração com o Spring Framework.
 * <p>
 * Registra o módulo {@link JavaTimeModule} para suporte a tipos de data e hora do Java 8 e
 * desabilita a serialização de datas como timestamps, garantindo que sejam serializadas em um formato legível.
 * Adiciona deserializadores customizados para {@link LocalDate} e {@link BigDecimal}.
 * </p>
 *
 * @author Marco Willy
 * @version 1.1
 * @since 2024
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Módulo para datas do Java 8
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer());
        mapper.registerModule(javaTimeModule);

        // Módulo customizado para BigDecimal
        SimpleModule customModule = new SimpleModule();
        customModule.addDeserializer(BigDecimal.class, new BigDecimalDeserializer());
        mapper.registerModule(customModule);

        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
