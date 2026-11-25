package cl.tupolera.catalog.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import cl.tupolera.catalog.security.ApiSecurityHandlers;
import cl.tupolera.catalog.security.InternalApiKeyFilter;

/**
 * Segunda cadena de seguridad, exclusiva de /internal/**, con prioridad sobre la cadena JWT existente.
 * Aquí el JWT no sirve: solo se acepta la cabecera X-Internal-Key (un token de ADMINISTRADOR no da acceso).
 * La cadena JWT de SecurityConfig sigue atendiendo todo lo demás sin cambios.
 */
@Configuration
@EnableConfigurationProperties(InternalApiProperties.class)
public class InternalSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain internalFilterChain(HttpSecurity http, InternalApiProperties props,
            ApiSecurityHandlers handlers) throws Exception {

        http
                .securityMatcher("/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new InternalApiKeyFilter(props, handlers), AuthorizationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().hasRole(InternalApiKeyFilter.ROL))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(handlers)
                        .accessDeniedHandler(handlers));

        return http.build();
    }
}
