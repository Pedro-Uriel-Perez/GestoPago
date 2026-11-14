package com.proyecto.servicios.security;

import com.proyecto.servicios.config.ZonaHoraria;
import com.proyecto.servicios.entity.clientes.Sesion;
import com.proyecto.servicios.repositorys.clientes.SesionRepository;
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
import java.time.LocalDateTime;
import java.util.List;

/**
 * Autentica cada request contra el JWT del header Authorization, pero no se
 * queda solo en validar la firma: tambien exige que la sesion a la que
 * pertenece el token (sesiones.identificador = claim "sid") siga activa y
 * que el token coincida con el guardado. Sin esto, un cierre de sesion
 * (manual o por inactividad) no tendria efecto real sobre la API mientras
 * el JWT no expirara por si solo.
 *
 * El principal de la autenticacion es el clienteId (Integer) y en "details"
 * queda el identificador de la sesion, para saber desde cual de las sesiones
 * del cliente (web o movil) viene la peticion.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Cada peticion autenticada cuenta como actividad, pero no hace falta
     * escribir en la base en cada una: basta con refrescar el ultimo acceso
     * cuando ya paso este margen.
     */
    private static final long SEGUNDOS_ENTRE_REFRESCOS = 30;

    /**
     * Las apps consultan esta ruta cada pocos segundos, en segundo plano,
     * para avisar de inicios de sesion en otro dispositivo. No es actividad
     * del usuario: si contara, una app abierta y olvidada nunca se cerraria
     * por inactividad.
     */
    private static final String RUTA_SIN_ACTIVIDAD = "/login/sesiones";

    private final JwtService jwtService;
    private final SesionRepository sesionRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith("Bearer ")) {
            autenticarSiTokenValido(header.substring(7), !RUTA_SIN_ACTIVIDAD.equals(request.getRequestURI()));
        }

        filterChain.doFilter(request, response);
    }

    private void autenticarSiTokenValido(String token, boolean cuentaComoActividad) {
        try {
            Integer clienteId = jwtService.obtenerClienteId(token);
            String sesionId = jwtService.obtenerSesionId(token);
            if (sesionId == null) {
                return;
            }
            sesionRepository.findByIdentificador(sesionId)
                    .filter(Sesion::getActiva)
                    .filter(sesion -> token.equals(sesion.getJwtToken()))
                    .ifPresent(sesion -> {
                        if (cuentaComoActividad) {
                            registrarActividad(sesion);
                        }
                        UsernamePasswordAuthenticationToken autenticacion =
                                new UsernamePasswordAuthenticationToken(clienteId, null, List.of());
                        autenticacion.setDetails(sesionId);
                        SecurityContextHolder.getContext().setAuthentication(autenticacion);
                    });
        } catch (JwtException | IllegalArgumentException ex) {
            // Token invalido, mal formado o expirado: se deja sin autenticar;
            // la regla de autorizacion de la ruta decide si eso es suficiente.
        }
    }

    private void registrarActividad(Sesion sesion) {
        LocalDateTime ahora = LocalDateTime.now(ZonaHoraria.MEXICO);
        if (sesion.getFechaUltimoAcceso().isBefore(ahora.minusSeconds(SEGUNDOS_ENTRE_REFRESCOS))) {
            sesion.setFechaUltimoAcceso(ahora);
            sesionRepository.save(sesion);
        }
    }
}
