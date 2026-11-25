
package cl.tupolera.gateway.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import cl.tupolera.gateway.security.ApiSecurityHandlers;

/**
 * Seguridad del Gateway: valida la firma y la vigencia del JWT en las rutas protegidas.
 * NO valida roles: cada microservicio aplica sus propias reglas de autorización.
 * El CORS se configura únicamente aquí (los microservicios no agregan encabezados CORS).
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ReactiveJwtDecoder jwtDecoder,
            ApiSecurityHandlers handlers,
            CorsConfigurationSource corsConfigurationSource) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeExchange(exchange -> exchange
                        // Preflight de CORS
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()

                        // Autenticación pública
                        .pathMatchers(HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout").permitAll()

                        // Los productos críticos son administrativos: requieren token.
                        // (Debe declararse antes que la regla pública de productos.)
                        .pathMatchers(HttpMethod.GET, "/api/v1/productos/criticos").authenticated()

                        // Catálogo y ubicaciones: lectura pública para la tienda
                        .pathMatchers(HttpMethod.GET,
                                "/api/v1/productos/**",
                                "/api/v1/categorias/**",
                                "/api/v1/regiones/**").permitAll()

                        // Documentación y salud
                        .pathMatchers(
                                "/docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/webjars/**",
                                "/v3/api-docs/**",
                                "/actuator/health").permitAll()

                        // Todo lo demás exige un JWT válido
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtDecoder(jwtDecoder))
                        .authenticationEntryPoint(handlers))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(handlers)
                        .accessDeniedHandler(handlers))
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties props) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(props.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));

        // El JWT viaja en el encabezado Authorization, no en cookies.
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
