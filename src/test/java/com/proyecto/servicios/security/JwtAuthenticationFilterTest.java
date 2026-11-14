package com.proyecto.servicios.security;

import com.proyecto.servicios.entity.clientes.Sesion;
import com.proyecto.servicios.repositorys.clientes.SesionRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "token-valido";

    @Mock
    private JwtService jwtService;

    @Mock
    private SesionRepository sesionRepository;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filtro;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private Authentication filtrarCon(String headerAuthorization) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (headerAuthorization != null) {
            request.addHeader("Authorization", headerAuthorization);
        }
        filtro.doFilter(request, new MockHttpServletResponse(), filterChain);
        verify(filterChain).doFilter(any(), any());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private Sesion sesion(String token, boolean activa, LocalDateTime ultimoAcceso) {
        Sesion sesion = new Sesion();
        sesion.setClienteId(7);
        sesion.setIdentificador("sid-web");
        sesion.setDispositivo("WEB");
        sesion.setJwtToken(token);
        sesion.setActiva(activa);
        sesion.setFechaUltimoAcceso(ultimoAcceso);
        return sesion;
    }

    private void tokenDeLaSesion(Sesion sesion) {
        when(jwtService.obtenerClienteId(TOKEN)).thenReturn(7);
        when(jwtService.obtenerSesionId(TOKEN)).thenReturn("sid-web");
        when(sesionRepository.findByIdentificador("sid-web")).thenReturn(Optional.ofNullable(sesion));
    }

    @Test
    void sesionActivaConElMismoToken_autenticaConClienteIdYDejaElIdDeSesion() throws Exception {
        tokenDeLaSesion(sesion(TOKEN, true, LocalDateTime.now().plusMinutes(1)));

        Authentication autenticacion = filtrarCon("Bearer " + TOKEN);

        assertThat(autenticacion.getPrincipal()).isEqualTo(7);
        assertThat(autenticacion.getDetails()).isEqualTo("sid-web");
        verify(sesionRepository, never()).save(any());
    }

    @Test
    void peticionAutenticada_refrescaElUltimoAccesoSiYaPasoElMargen() throws Exception {
        Sesion sesion = sesion(TOKEN, true, LocalDateTime.of(2020, 1, 1, 0, 0));
        tokenDeLaSesion(sesion);

        filtrarCon("Bearer " + TOKEN);

        assertThat(sesion.getFechaUltimoAcceso()).isAfter(LocalDateTime.of(2020, 1, 1, 0, 0));
        verify(sesionRepository).save(sesion);
    }

    @Test
    void consultaDeSesionesEnSegundoPlano_autenticaPeroNoCuentaComoActividad() throws Exception {
        Sesion sesion = sesion(TOKEN, true, LocalDateTime.of(2020, 1, 1, 0, 0));
        tokenDeLaSesion(sesion);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/sesiones");
        request.addHeader("Authorization", "Bearer " + TOKEN);

        filtro.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(7);
        assertThat(sesion.getFechaUltimoAcceso()).isEqualTo(LocalDateTime.of(2020, 1, 1, 0, 0));
        verify(sesionRepository, never()).save(any());
    }

    @Test
    void sesionCerrada_noAutentica() throws Exception {
        tokenDeLaSesion(sesion(null, false, LocalDateTime.now()));

        assertThat(filtrarCon("Bearer " + TOKEN)).isNull();
    }

    @Test
    void tokenQueYaNoEsElDeLaSesion_noAutentica() throws Exception {
        tokenDeLaSesion(sesion("otro-token-mas-nuevo", true, LocalDateTime.now()));

        assertThat(filtrarCon("Bearer " + TOKEN)).isNull();
    }

    @Test
    void sesionInexistente_noAutentica() throws Exception {
        tokenDeLaSesion(null);

        assertThat(filtrarCon("Bearer " + TOKEN)).isNull();
    }

    @Test
    void tokenSinIdDeSesion_noAutentica() throws Exception {
        when(jwtService.obtenerClienteId(TOKEN)).thenReturn(7);
        when(jwtService.obtenerSesionId(TOKEN)).thenReturn(null);

        assertThat(filtrarCon("Bearer " + TOKEN)).isNull();
        verify(sesionRepository, never()).findByIdentificador(any());
    }

    @Test
    void tokenInvalido_noAutenticaYDejaSeguirLaPeticion() throws Exception {
        when(jwtService.obtenerClienteId(TOKEN)).thenThrow(new JwtException("firma invalida"));

        assertThat(filtrarCon("Bearer " + TOKEN)).isNull();
    }

    @Test
    void sinHeaderOConOtroEsquema_noIntentaAutenticar() throws Exception {
        assertThat(filtrarCon(null)).isNull();
        verify(jwtService, never()).obtenerClienteId(any());
    }

    @Test
    void headerQueNoEsBearer_noIntentaAutenticar() throws Exception {
        assertThat(filtrarCon("Basic abc")).isNull();
        verify(jwtService, never()).obtenerClienteId(any());
    }
}
