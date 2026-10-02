package com.proyecto.servicios.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Map;

/**
 * El JWT que emite POST /login antes no protegia nada: cualquiera podia
 * llamar /clientes o /cuentas sin token. Aqui se exige un JWT valido (ver
 * JwtAuthenticationFilter) para los endpoints de Onboarding de Clientes.
 *
 * Las rutas de otras tareas (/productos, /personas*) y Swagger se dejan
 * publicas a proposito: no formaban parte de este enunciado y cambiar su
 * comportamiento estaria fuera de alcance.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS = {
            "/productos", "/personas", "/personasActualiza", "/personasElimina",
            "/actuator/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .requestMatchers(HttpMethod.POST, "/clientes").permitAll()
                        .requestMatchers(HttpMethod.POST, "/login").permitAll()
                        .requestMatchers("/clientes/**", "/cuentas/**", "/login/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(eh -> eh.authenticationEntryPoint(this::responderNoAutenticado))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void responderNoAutenticado(jakarta.servlet.http.HttpServletRequest request,
                                         jakarta.servlet.http.HttpServletResponse response,
                                         org.springframework.security.core.AuthenticationException ex) throws java.io.IOException {
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                Map.of("codigo", 401, "mensaje", "Token invalido, ausente o sesion expirada"));
    }
}
