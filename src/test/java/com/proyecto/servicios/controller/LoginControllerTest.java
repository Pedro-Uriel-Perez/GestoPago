package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.clientes.LoginRequest;
import com.proyecto.servicios.model.clientes.LoginResponse;
import com.proyecto.servicios.service.LoginService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private LoginService loginService;

    @InjectMocks
    private LoginController loginController;

    @Test
    void iniciarSesion_regresa200ConElJwt() {
        LoginRequest request = new LoginRequest();
        request.setCorreoElectronico("pedro@correo.com");
        request.setPassword("Secreta123");
        when(loginService.iniciarSesion("pedro@correo.com", "Secreta123")).thenReturn("jwt-generado");

        ResponseEntity<LoginResponse> respuesta = loginController.iniciarSesion(request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().getJwt()).isEqualTo("jwt-generado");
    }

    @Test
    void cerrarSesion_regresa204YCierraLaSesionDelCliente() {
        ResponseEntity<Void> respuesta = loginController.cerrarSesion(8);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(loginService).cerrarSesion(8);
    }
}
