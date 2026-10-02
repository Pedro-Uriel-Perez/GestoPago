package com.proyecto.servicios.security;

import com.proyecto.servicios.entity.clientes.Login;
import com.proyecto.servicios.repositorys.clientes.LoginRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Autentica cada request contra el JWT del header Authorization, pero no se
 * queda solo en validar la firma: tambien exige que logins.sesion_activa
 * siga en true y que el token coincida con el guardado. Sin esto, un cierre
 * de sesion (manual o por inactividad) no tendria efecto real sobre la API
 * mientras el JWT no expirara por si solo.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final LoginRepository loginRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith("Bearer ")) {
            autenticarSiTokenValido(header.substring(7));
        }

        filterChain.doFilter(request, response);
    }

    private void autenticarSiTokenValido(String token) {
        try {
            Integer clienteId = jwtService.obtenerClienteId(token);
            loginRepository.findByClienteId(clienteId)
                    .filter(Login::getSesionActiva)
                    .filter(login -> token.equals(login.getJwtToken()))
                    .ifPresent(login -> SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(clienteId, null, List.of())));
        } catch (JwtException | IllegalArgumentException ex) {
            // Token invalido, mal formado o expirado: se deja sin autenticar;
            // la regla de autorizacion de la ruta decide si eso es suficiente.
        }
    }
}
