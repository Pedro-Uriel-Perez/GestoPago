package com.proyecto.servicios.config;

import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Interceptores de Feign que agregan el token Bearer de GestoPago (y el
 * X-API-Key opcional) a cada llamada del catalogo de productos y de
 * sendTx/confirmTx.
 */
@ExtendWith(MockitoExtension.class)
class GestoPagoFeignConfigTest {

    private static final Integer ID_DISTRIBUIDOR = 83;
    private static final String CODIGO_DISPOSITIVO = "GPS83-TPV-17";

    @Mock
    private GestoPagoTokenService tokenService;

    private void tokenVigente(String valor) {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken(valor);
        when(tokenService.obtenerTokenActivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO)).thenReturn(Optional.of(token));
    }

    private RequestInterceptor interceptorCatalogo() {
        GestoPagoProductListFeignConfig config = new GestoPagoProductListFeignConfig(tokenService);
        ReflectionTestUtils.setField(config, "idDistribuidor", ID_DISTRIBUIDOR);
        ReflectionTestUtils.setField(config, "codigoDispositivo", CODIGO_DISPOSITIVO);
        return config.gestoPagoProductListAuthInterceptor();
    }

    private RequestInterceptor interceptorSenTx(String apiKey) {
        GestoPagoSenTxFeignConfig config = new GestoPagoSenTxFeignConfig(tokenService);
        ReflectionTestUtils.setField(config, "idDistribuidor", ID_DISTRIBUIDOR);
        ReflectionTestUtils.setField(config, "codigoDispositivo", CODIGO_DISPOSITIVO);
        ReflectionTestUtils.setField(config, "apiKey", apiKey);
        return config.gestoPagoSenTxAuthInterceptor();
    }

    @Test
    void catalogo_conTokenVigente_agregaElHeaderBearer() {
        tokenVigente("token-123");
        RequestTemplate peticion = new RequestTemplate();

        interceptorCatalogo().apply(peticion);

        assertThat(peticion.headers().get("Authorization")).containsExactly("Bearer token-123");
    }

    @Test
    void catalogo_sinTokenVigente_lanza503() {
        when(tokenService.obtenerTokenActivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO)).thenReturn(Optional.empty());
        RequestInterceptor interceptor = interceptorCatalogo();
        RequestTemplate peticion = new RequestTemplate();

        assertThatThrownBy(() -> interceptor.apply(peticion))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void sendTx_conApiKey_agregaBearerYXApiKey() {
        tokenVigente("token-123");
        RequestTemplate peticion = new RequestTemplate();

        interceptorSenTx("mi-api-key").apply(peticion);

        assertThat(peticion.headers().get("Authorization")).containsExactly("Bearer token-123");
        assertThat(peticion.headers().get("X-API-Key")).containsExactly("mi-api-key");
    }

    @Test
    void sendTx_sinApiKey_soloAgregaBearer() {
        tokenVigente("token-123");
        RequestTemplate peticion = new RequestTemplate();

        interceptorSenTx("").apply(peticion);

        assertThat(peticion.headers().get("Authorization")).containsExactly("Bearer token-123");
        assertThat(peticion.headers()).doesNotContainKey("X-API-Key");
    }

    @Test
    void sendTx_sinTokenVigente_lanza503() {
        when(tokenService.obtenerTokenActivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO)).thenReturn(Optional.empty());
        RequestInterceptor interceptor = interceptorSenTx("");
        RequestTemplate peticion = new RequestTemplate();

        assertThatThrownBy(() -> interceptor.apply(peticion))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.SERVICE_UNAVAILABLE);
    }
}
