package br.com.groupsoftware.grouppay.extratoremail.util;


import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;

public class RestUtil {

    /**
     * Cria os cabeçalhos HTTP com autorização e content-type.
     *
     * @param token Token de autenticação.
     * @return HttpHeaders configurado.
     */
    public  static HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }


    public  static String buildUrl(String host, String basePath, String endpoint) {
        return buildUrl(host, basePath, endpoint, null);
    }
    /**
     * Constrói a URL completa para o endpoint, concatenando o host, o caminho base e
     * o endpoint específico. Se o parâmetro id for informado, ele é adicionado
     * na URL como parte do path.
     *
     * @param endpoint     O endpoint (por exemplo, PATH_ENVIO_DOCUMENTO ou PATH_CONSULTA_RESPOSTA).
     * @param resourceId   O identificador do recurso (pode ser nulo para chamadas que não precisam de id no path).
     * @return A URL completa.
     */
    public  static String buildUrl(String host, String basePath, String endpoint, Long resourceId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(host + basePath + endpoint);
        if (resourceId != null) {
            builder.path(String.valueOf(resourceId));
        }
        return builder.toUriString();
    }
}
