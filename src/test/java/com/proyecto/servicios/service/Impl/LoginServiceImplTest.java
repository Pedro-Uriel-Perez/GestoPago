package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.entity.clientes.Login;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.repositorys.clientes.ClienteRepository;
import com.proyecto.servicios.repositorys.clientes.LoginRepository;
import com.proyecto.servicios.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private LoginRepository loginRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private LoginServiceImpl loginService;

    @Test
    void iniciarSesion_clienteExistente_creaLoginConSesionActivaYToken() {
        Cliente cliente = new Cliente();
        cliente.setId(1);
        cliente.setCorreoElectronico("cliente@correo.com");
        cliente.setNombre("Ana");

        when(clienteRepository.findByCorreoElectronico("cliente@correo.com")).thenReturn(Optional.of(cliente));
        when(jwtService.generarToken(1, "cliente@correo.com")).thenReturn("token-generado");
        when(loginRepository.findByClienteId(1)).thenReturn(Optional.empty());

        String token = loginService.iniciarSesion("cliente@correo.com");

        assertThat(token).isEqualTo("token-generado");

        ArgumentCaptor<Login> captor = ArgumentCaptor.forClass(Login.class);
        verify(loginRepository).save(captor.capture());
        assertThat(captor.getValue().getSesionActiva()).isTrue();
        assertThat(captor.getValue().getJwtToken()).isEqualTo("token-generado");
        assertThat(captor.getValue().getFechaUltimoAcceso()).isNotNull();
    }

    @Test
    void iniciarSesion_clienteNoExiste_lanzaClienteNoEncontradoException() {
        when(clienteRepository.findByCorreoElectronico("no-existe@correo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.iniciarSesion("no-existe@correo.com"))
                .isInstanceOf(ClienteNoEncontradoException.class);

        verify(loginRepository, never()).save(any());
    }

    @Test
    void cerrarSesion_loginExistente_loMarcaComoInactivoYBorraElToken() {
        Login login = new Login();
        login.setSesionActiva(true);
        login.setJwtToken("token-viejo");

        when(loginRepository.findByClienteId(1)).thenReturn(Optional.of(login));

        loginService.cerrarSesion(1);

        assertThat(login.getSesionActiva()).isFalse();
        assertThat(login.getJwtToken()).isNull();
        verify(loginRepository).save(login);
    }

    @Test
    void cerrarSesionesInactivas_cierraSoloLasQueSuperanElUmbral() {
        Login sesionInactiva1 = new Login();
        sesionInactiva1.setSesionActiva(true);
        sesionInactiva1.setJwtToken("token-1");
        sesionInactiva1.setFechaUltimoAcceso(LocalDateTime.now().minusMinutes(10));

        Login sesionInactiva2 = new Login();
        sesionInactiva2.setSesionActiva(true);
        sesionInactiva2.setJwtToken("token-2");

        when(loginRepository.findBySesionActivaTrueAndFechaUltimoAccesoBefore(any()))
                .thenReturn(List.of(sesionInactiva1, sesionInactiva2));

        loginService.cerrarSesionesInactivas();

        assertThat(sesionInactiva1.getSesionActiva()).isFalse();
        assertThat(sesionInactiva1.getJwtToken()).isNull();
        assertThat(sesionInactiva2.getSesionActiva()).isFalse();
        verify(loginRepository).saveAll(List.of(sesionInactiva1, sesionInactiva2));
    }
}
