package com.proyecto.servicios.controller;

import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.model.clientes.LoginRequest;
import com.proyecto.servicios.model.clientes.LoginResponse;
import com.proyecto.servicios.model.clientes.SesionResponse;
import com.proyecto.servicios.service.LoginService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private LoginService loginService;

    @InjectMocks
    private LoginController loginController;

    /** Autenticacion como la deja JwtAuthenticationFilter: clienteId + id de sesion. */
    private static Authentication autenticacion(Integer clienteId, String sesionId) {
        UsernamePasswordAuthenticationToken autenticacion =
                new UsernamePasswordAuthenticationToken(clienteId, null, List.of());
        autenticacion.setDetails(sesionId);
        return autenticacion;
    }

    @Test
    void iniciarSesion_regresa200ConElJwtDelDispositivo() {
        LoginRequest request = new LoginRequest();
        request.setCorreoElectronico("pedro@correo.com");
        request.setPassword("Secreta123");
        request.setDispositivo("MOVIL");
        when(loginService.iniciarSesion("pedro@correo.com", "Secreta123", "MOVIL")).thenReturn("jwt-generado");

        ResponseEntity<LoginResponse> respuesta = loginController.iniciarSesion(request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().getJwt()).isEqualTo("jwt-generado");
    }

    @Test
    void cerrarSesion_regresa204YCierraSoloLaSesionDeEsteDispositivo() {
        ResponseEntity<Void> respuesta = loginController.cerrarSesion(8, autenticacion(8, "sid-web"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(loginService).cerrarSesion(8, "sid-web");
    }

    @Test
    void cerrarSesion_deOtroCliente_lanzaError403() {
        Authentication autenticacion = autenticacion(8, "sid-web");

        assertThatThrownBy(() -> loginController.cerrarSesion(9, autenticacion))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
        verify(loginService, never()).cerrarSesion(any(), any());
    }

    @Test
    void obtenerSesiones_regresa200ConLasSesionesDelClienteAutenticado() {
        SesionResponse movil = new SesionResponse("MOVIL", LocalDateTime.now(), LocalDateTime.now(), false);
        when(loginService.obtenerSesionesActivas(8, "sid-web")).thenReturn(List.of(movil));

        ResponseEntity<List<SesionResponse>> respuesta = loginController.obtenerSesiones(autenticacion(8, "sid-web"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsExactly(movil);
    }
}
