package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.entity.clientes.Login;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.repositorys.clientes.ClienteRepository;
import com.proyecto.servicios.repositorys.clientes.LoginRepository;
import com.proyecto.servicios.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

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
    void registrarCredenciales_guardaLoginConHashDeLaContrasenaYSesionInactiva() {
        Cliente cliente = new Cliente();
        cliente.setId(1);
        cliente.setCorreoElectronico("cliente@correo.com");
        cliente.setNombre("Ana");

        loginService.registrarCredenciales(cliente, "contrasena123");

        ArgumentCaptor<Login> captor = ArgumentCaptor.forClass(Login.class);
        verify(loginRepository).save(captor.capture());

        Login login = captor.getValue();
        assertThat(login.getPasswordHash()).isNotEqualTo("contrasena123");
        assertThat(new BCryptPasswordEncoder().matches("contrasena123", login.getPasswordHash())).isTrue();
        assertThat(login.getSesionActiva()).isFalse();
    }

    @Test
    void iniciarSesion_credencialesCorrectas_creaSesionActivaYToken() {
        Cliente cliente = new Cliente();
        cliente.setId(1);
        cliente.setCorreoElectronico("cliente@correo.com");
        cliente.setNombre("Ana");

        Login login = new Login();
        login.setPasswordHash(new BCryptPasswordEncoder().encode("contrasena123"));

        when(clienteRepository.findByCorreoElectronico("cliente@correo.com")).thenReturn(Optional.of(cliente));
        when(loginRepository.findByClienteId(1)).thenReturn(Optional.of(login));
        when(jwtService.generarToken(1, "cliente@correo.com")).thenReturn("token-generado");

        String token = loginService.iniciarSesion("cliente@correo.com", "contrasena123");

        assertThat(token).isEqualTo("token-generado");
        assertThat(login.getSesionActiva()).isTrue();
        assertThat(login.getJwtToken()).isEqualTo("token-generado");
        assertThat(login.getFechaUltimoAcceso()).isNotNull();
        verify(loginRepository).save(login);
    }

    @Test
    void iniciarSesion_contrasenaIncorrecta_lanzaCredencialesInvalidasException() {
        Cliente cliente = new Cliente();
        cliente.setId(1);
        cliente.setCorreoElectronico("cliente@correo.com");

        Login login = new Login();
        login.setPasswordHash(new BCryptPasswordEncoder().encode("contrasena123"));

        when(clienteRepository.findByCorreoElectronico("cliente@correo.com")).thenReturn(Optional.of(cliente));
        when(loginRepository.findByClienteId(1)).thenReturn(Optional.of(login));

        assertThatThrownBy(() -> loginService.iniciarSesion("cliente@correo.com", "contrasena-equivocada"))
                .isInstanceOf(CredencialesInvalidasException.class);

        verify(loginRepository, never()).save(any());
    }

    @Test
    void iniciarSesion_clienteDadoDeBaja_lanzaCredencialesInvalidasException() {
        Cliente cliente = new Cliente();
        cliente.setId(1);
        cliente.setCorreoElectronico("cliente@correo.com");
        cliente.setActivo(false);

        when(clienteRepository.findByCorreoElectronico("cliente@correo.com")).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> loginService.iniciarSesion("cliente@correo.com", "contrasena123"))
                .isInstanceOf(CredencialesInvalidasException.class);

        verify(loginRepository, never()).findByClienteId(any());
        verify(loginRepository, never()).save(any());
    }

    @Test
    void iniciarSesion_correoNoExiste_lanzaCredencialesInvalidasException() {
        when(clienteRepository.findByCorreoElectronico("no-existe@correo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.iniciarSesion("no-existe@correo.com", "cualquier-cosa"))
                .isInstanceOf(CredencialesInvalidasException.class);

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
        ReflectionTestUtils.setField(loginService, "inactividadMinutos", 5L);

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
