package com.proyecto.servicios.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
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
            "/actuator/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
            "/nacionalidades"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Revisado (Sonar java:S4502): CSRF solo aplica cuando la
                // sesion viaja en cookies que el navegador manda solo. Esta
                // API es stateless y se autentica con el JWT en el header
                // Authorization, que el navegador nunca adjunta por su cuenta,
                // asi que no hay token que un sitio ajeno pueda reutilizar.
                .csrf(csrf -> csrf.disable()) // NOSONAR
                // Usa la configuracion de CorsConfig; asi el preflight
                // (OPTIONS) del navegador se responde antes de pedir el JWT.
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .requestMatchers(HttpMethod.POST, "/clientes").permitAll()
                        .requestMatchers(HttpMethod.POST, "/login").permitAll()
                        .requestMatchers("/clientes/**", "/cuentas/**", "/login/**", "/compras/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(eh -> eh.authenticationEntryPoint(
                        (request, response, ex) -> responderNoAutenticado(response)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void responderNoAutenticado(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                Map.of("codigo", 401, "mensaje", "Token invalido, ausente o sesion expirada"));
    }
}
