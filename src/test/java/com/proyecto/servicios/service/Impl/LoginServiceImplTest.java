package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.entity.clientes.Login;
import com.proyecto.servicios.entity.clientes.Sesion;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.model.clientes.SesionResponse;
import com.proyecto.servicios.repositorys.clientes.ClienteRepository;
import com.proyecto.servicios.repositorys.clientes.LoginRepository;
import com.proyecto.servicios.repositorys.clientes.SesionRepository;
import com.proyecto.servicios.security.JwtService;
import com.proyecto.servicios.service.MailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceImplTest {

    private static final String CORREO = "cliente@correo.com";
    private static final String PASSWORD = "contrasena123";

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private LoginRepository loginRepository;

    @Mock
    private SesionRepository sesionRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private MailService mailService;

    @InjectMocks
    private LoginServiceImpl loginService;

    private Cliente cliente() {
        Cliente cliente = new Cliente();
        cliente.setId(1);
        cliente.setCorreoElectronico(CORREO);
        cliente.setNombre("Ana");
        return cliente;
    }

    private Sesion sesionActiva(String identificador, String dispositivo) {
        Sesion sesion = new Sesion();
        sesion.setClienteId(1);
        sesion.setIdentificador(identificador);
        sesion.setDispositivo(dispositivo);
        sesion.setJwtToken("token-" + identificador);
        sesion.setFechaInicio(LocalDateTime.of(2026, 11, 13, 10, 0));
        sesion.setFechaUltimoAcceso(LocalDateTime.of(2026, 11, 13, 10, 5));
        return sesion;
    }

    private void credencialesValidas(List<Sesion> sesionesPrevias) {
        Login login = new Login();
        login.setPasswordHash(new BCryptPasswordEncoder().encode(PASSWORD));
        when(clienteRepository.findByCorreoElectronico(CORREO)).thenReturn(Optional.of(cliente()));
        when(loginRepository.findByClienteId(1)).thenReturn(Optional.of(login));
        when(sesionRepository.findByClienteIdAndActivaTrueOrderByFechaInicioDesc(1)).thenReturn(sesionesPrevias);
        when(jwtService.generarToken(eq(1), eq(CORREO), anyString())).thenReturn("token-generado");
    }

    private Sesion sesionGuardada() {
        ArgumentCaptor<Sesion> captor = ArgumentCaptor.forClass(Sesion.class);
        verify(sesionRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void registrarCredenciales_guardaLoginConHashDeLaContrasenaYSesionInactiva() {
        loginService.registrarCredenciales(cliente(), PASSWORD);

        ArgumentCaptor<Login> captor = ArgumentCaptor.forClass(Login.class);
        verify(loginRepository).save(captor.capture());

        Login login = captor.getValue();
        assertThat(login.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(new BCryptPasswordEncoder().matches(PASSWORD, login.getPasswordHash())).isTrue();
        assertThat(login.getSesionActiva()).isFalse();
    }

    @Test
    void iniciarSesion_primeraSesion_creaSesionWebPorDefectoSinMandarAlerta() {
        credencialesValidas(List.of());

        String token = loginService.iniciarSesion(CORREO, PASSWORD, null);

        assertThat(token).isEqualTo("token-generado");
        Sesion sesion = sesionGuardada();
        assertThat(sesion.getDispositivo()).isEqualTo("WEB");
        assertThat(sesion.getActiva()).isTrue();
        assertThat(sesion.getJwtToken()).isEqualTo("token-generado");
        assertThat(sesion.getIdentificador()).hasSize(36);
        verify(mailService, never()).enviarAlertaInicioSesion(any(), any(), any());
    }

    @Test
    void iniciarSesion_enMovilConSesionWebAbierta_mantieneLaWebYMandaAlerta() {
        Sesion web = sesionActiva("sid-web", "WEB");
        credencialesValidas(List.of(web));

        loginService.iniciarSesion(CORREO, PASSWORD, "MOVIL");

        assertThat(web.getActiva()).isTrue();
        assertThat(sesionGuardada().getDispositivo()).isEqualTo("MOVIL");
        verify(mailService).enviarAlertaInicioSesion(eq(CORREO), eq("MOVIL"), any(LocalDateTime.class));
    }

    @Test
    void iniciarSesion_enElMismoDispositivo_reemplazaLaSesionAnterior() {
        Sesion webAnterior = sesionActiva("sid-web", "WEB");
        credencialesValidas(List.of(webAnterior));

        loginService.iniciarSesion(CORREO, PASSWORD, "WEB");

        assertThat(webAnterior.getActiva()).isFalse();
        assertThat(webAnterior.getJwtToken()).isNull();
        verify(sesionRepository).saveAll(List.of(webAnterior));
    }

    @Test
    void iniciarSesion_siFallaElCorreoDeAlerta_igualEntregaElToken() {
        credencialesValidas(List.of(sesionActiva("sid-web", "WEB")));
        doThrow(new MailSendException("smtp caido"))
                .when(mailService).enviarAlertaInicioSesion(any(), any(), any());

        assertThat(loginService.iniciarSesion(CORREO, PASSWORD, "MOVIL")).isEqualTo("token-generado");
    }

    @Test
    void iniciarSesion_contrasenaIncorrecta_lanzaError401() {
        Login login = new Login();
        login.setPasswordHash(new BCryptPasswordEncoder().encode(PASSWORD));
        when(clienteRepository.findByCorreoElectronico(CORREO)).thenReturn(Optional.of(cliente()));
        when(loginRepository.findByClienteId(1)).thenReturn(Optional.of(login));

        assertThatThrownBy(() -> loginService.iniciarSesion(CORREO, "contrasena-equivocada", "WEB"))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
        verify(sesionRepository, never()).save(any());
    }

    @Test
    void iniciarSesion_clienteSinRegistroDeLogin_lanzaError401() {
        when(clienteRepository.findByCorreoElectronico(CORREO)).thenReturn(Optional.of(cliente()));
        when(loginRepository.findByClienteId(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.iniciarSesion(CORREO, PASSWORD, "WEB"))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
    }

    @Test
    void iniciarSesion_clienteInexistente_lanzaError401() {
        when(clienteRepository.findByCorreoElectronico("no-existe@correo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.iniciarSesion("no-existe@correo.com", "cualquier-cosa", "WEB"))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
    }

    @Test
    void iniciarSesion_clienteDadoDeBaja_lanzaError401() {
        Cliente inactivo = cliente();
        inactivo.setActivo(false);
        when(clienteRepository.findByCorreoElectronico(CORREO)).thenReturn(Optional.of(inactivo));

        assertThatThrownBy(() -> loginService.iniciarSesion(CORREO, PASSWORD, "WEB"))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
    }

    @Test
    void cerrarSesion_cierraSoloLaSesionIndicada() {
        Sesion movil = sesionActiva("sid-movil", "MOVIL");
        when(sesionRepository.findByIdentificador("sid-movil")).thenReturn(Optional.of(movil));

        loginService.cerrarSesion(1, "sid-movil");

        assertThat(movil.getActiva()).isFalse();
        assertThat(movil.getJwtToken()).isNull();
        verify(sesionRepository).save(movil);
    }

    @Test
    void cerrarSesion_deOtroCliente_noLaToca() {
        Sesion ajena = sesionActiva("sid-ajena", "WEB");
        when(sesionRepository.findByIdentificador("sid-ajena")).thenReturn(Optional.of(ajena));

        loginService.cerrarSesion(99, "sid-ajena");

        assertThat(ajena.getActiva()).isTrue();
        verify(sesionRepository, never()).save(any());
    }

    @Test
    void cerrarTodasLasSesiones_cierraWebYMovil() {
        Sesion web = sesionActiva("sid-web", "WEB");
        Sesion movil = sesionActiva("sid-movil", "MOVIL");
        when(sesionRepository.findByClienteIdAndActivaTrueOrderByFechaInicioDesc(1)).thenReturn(List.of(web, movil));

        loginService.cerrarTodasLasSesiones(1);

        assertThat(web.getActiva()).isFalse();
        assertThat(movil.getActiva()).isFalse();
        verify(sesionRepository).saveAll(List.of(web, movil));
    }

    @Test
    void obtenerSesionesActivas_marcaCualEsLaSesionActual() {
        when(sesionRepository.findByClienteIdAndActivaTrueOrderByFechaInicioDesc(1))
                .thenReturn(List.of(sesionActiva("sid-movil", "MOVIL"), sesionActiva("sid-web", "WEB")));

        List<SesionResponse> sesiones = loginService.obtenerSesionesActivas(1, "sid-web");

        assertThat(sesiones).hasSize(2);
        assertThat(sesiones.get(0).getDispositivo()).isEqualTo("MOVIL");
        assertThat(sesiones.get(0).isActual()).isFalse();
        assertThat(sesiones.get(1).getDispositivo()).isEqualTo("WEB");
        assertThat(sesiones.get(1).isActual()).isTrue();
        assertThat(sesiones.get(1).getFechaInicio()).isEqualTo(LocalDateTime.of(2026, 11, 13, 10, 0));
    }

    @Test
    void cerrarSesionesInactivas_cierraLasQueSuperanElUmbral() {
        ReflectionTestUtils.setField(loginService, "inactividadMinutos", 5L);
        Sesion inactiva = sesionActiva("sid-web", "WEB");
        when(sesionRepository.findByActivaTrueAndFechaUltimoAccesoBefore(any())).thenReturn(List.of(inactiva));

        loginService.cerrarSesionesInactivas();

        assertThat(inactiva.getActiva()).isFalse();
        assertThat(inactiva.getJwtToken()).isNull();
        verify(sesionRepository).saveAll(List.of(inactiva));
    }
}
