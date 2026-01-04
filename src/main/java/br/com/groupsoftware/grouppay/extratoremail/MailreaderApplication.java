package br.com.groupsoftware.grouppay.extratoremail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Classe principal da aplicação Mailreader.
 * <p>
 * Esta classe inicializa a aplicação Spring Boot, configurando-a para escanear pacotes e habilitar a execução de tarefas agendadas.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@SpringBootApplication
public class MailreaderApplication {

    public static void main(String[] args) {
        SpringApplication.run(MailreaderApplication.class, args);
    }

}
