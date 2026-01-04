package br.com.groupsoftware.grouppay.extratoremail.config;


import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Configuração para o Bean do {@link RestTemplate}.
 * <p>
 * Esta classe fornece uma configuração centralizada para instanciar
 * e gerenciar o Bean do {@link RestTemplate} utilizando o Apache HttpClient,
 * o que permite o uso de um pool de conexões e configuração de timeouts
 * para melhorar a performance e a robustez nas chamadas HTTP externas.
 * </p>
 *
 * @author Marco
 * @version 2.0
 * @since 2024
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(50);
        connectionManager.setDefaultMaxPerRoute(20);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.of(3000, TimeUnit.MILLISECONDS))
                .setConnectTimeout(Timeout.of(3000, TimeUnit.MILLISECONDS))
                .setResponseTimeout(Timeout.of(5000, TimeUnit.MILLISECONDS))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(requestFactory);
    }
}
