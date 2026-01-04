package br.com.groupsoftware.grouppay.extratoremail;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Classe para inicialização da aplicação em um ambiente de servlet.
 * <p>
 * Esta classe é utilizada quando a aplicação Spring Boot é implantada em um servidor de servlet, como Tomcat, WildFly ou Jetty.
 * Ela configura a aplicação Spring Boot, permitindo que ela seja executada em um ambiente baseado em servlet.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(MailreaderApplication.class);
    }

}
