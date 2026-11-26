package cl.tupolera.sales.client;

import java.net.http.HttpClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Crea un RestClient por servicio remoto, con tiempos máximos de conexión y de lectura. */
@Configuration
public class ClientesConfig {

    @Bean
    public RestClient catalogoRestClient(RestClient.Builder builder, ClientesProperties props) {
        return construir(builder, props, props.catalogoUrl());
    }

    @Bean
    public RestClient authRestClient(RestClient.Builder builder, ClientesProperties props) {
        return construir(builder, props, props.authUrl());
    }

    private static RestClient construir(RestClient.Builder builder, ClientesProperties props, String baseUrl) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(props.timeoutConexion()).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(props.timeoutLectura());
        return builder.baseUrl(baseUrl).requestFactory(fabrica).build();
    }
}
