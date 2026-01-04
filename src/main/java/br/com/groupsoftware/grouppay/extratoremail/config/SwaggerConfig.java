package br.com.groupsoftware.grouppay.extratoremail.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração da documentação Swagger/OpenAPI para a API GroupPay MailReader.
 * <p>
 * Define as informações básicas da API, como título, versão e descrição,
 * para serem exibidas na interface de documentação gerada pelo Swagger/OpenAPI.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("API GroupPay MailReader")
                        .version("1.0")
                        .description("Documentação da API GroupPay MailReader"));
    }
}



