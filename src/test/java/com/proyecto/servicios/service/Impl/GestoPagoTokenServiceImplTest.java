package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestoPagoTokenServiceImplTest {

    private static final Integer ID_DISTRIBUIDOR = 83;
    private static final String CODIGO_DISPOSITIVO = "GPS83-TPV-17";

    @Mock
    private GestoPagoAuthClient authClient;

    @Mock
    private GestoPagoTokenRepository tokenRepository;

    @Mock
    private GestoPagoTokenMapper tokenMapper;

    @InjectMocks
    private GestoPagoTokenServiceImpl tokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenService, "idDistribuidor", ID_DISTRIBUIDOR);
        ReflectionTestUtils.setField(tokenService, "codigoDispositivo", CODIGO_DISPOSITIVO);
        ReflectionTestUtils.setField(tokenService, "password", "clave-de-prueba");
    }

    private GestoPagoAuthResponse respuestaConToken(String token) {
        GestoPagoAuthResponse respuesta = new GestoPagoAuthResponse();
        respuesta.setToken(token);
        return respuesta;
    }

    @Test
    void renovarToken_primeraVez_creaUnTokenNuevoParaElDistribuidor() {
        GestoPagoAuthResponse respuesta = respuestaConToken("token-nuevo");
        when(authClient.authenticate(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO, "clave-de-prueba")).thenReturn(respuesta);
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.empty());
        GestoPagoToken nuevo = new GestoPagoToken();
        nuevo.setToken("token-nuevo");
        when(tokenMapper.toEntity(respuesta)).thenReturn(nuevo);

        tokenService.renovarToken();

        ArgumentCaptor<GestoPagoToken> guardado = ArgumentCaptor.forClass(GestoPagoToken.class);
        verify(tokenRepository).save(guardado.capture());
        assertThat(guardado.getValue().getIdDistribuidor()).isEqualTo(ID_DISTRIBUIDOR);
        assertThat(guardado.getValue().getCodigoDispositivo()).isEqualTo(CODIGO_DISPOSITIVO);
        assertThat(guardado.getValue().getActivo()).isTrue();
    }

    @Test
    void renovarToken_yaExistiaUnToken_loActualizaEnVezDeCrearOtro() {
        GestoPagoAuthResponse respuesta = respuestaConToken("token-renovado");
        GestoPagoToken existente = new GestoPagoToken();
        existente.setId(1);
        when(authClient.authenticate(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO, "clave-de-prueba")).thenReturn(respuesta);
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.of(existente));

        tokenService.renovarToken();

        verify(tokenMapper).updateEntity(respuesta, existente);
        verify(tokenMapper, never()).toEntity(any());
        verify(tokenRepository).save(existente);
    }

    @Test
    void renovarToken_respuestaSinToken_noGuardaNada() {
        when(authClient.authenticate(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO, "clave-de-prueba"))
                .thenReturn(respuestaConToken(null));

        tokenService.renovarToken();

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void renovarToken_respuestaNula_noGuardaNada() {
        when(authClient.authenticate(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO, "clave-de-prueba")).thenReturn(null);

        tokenService.renovarToken();

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void renovarToken_gestoPagoNoResponde_noRompeLaTareaProgramada() {
        when(authClient.authenticate(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO, "clave-de-prueba"))
                .thenThrow(new IllegalStateException("sin conexion"));

        assertThatCode(() -> tokenService.renovarToken()).doesNotThrowAnyException();

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void obtenerTokenActivo_regresaElTokenGuardado() {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("token-vigente");
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.of(token));

        Optional<GestoPagoToken> resultado = tokenService.obtenerTokenActivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO);

        assertThat(resultado).map(GestoPagoToken::getToken).contains("token-vigente");
    }
}
