package com.proyecto.servicios.config;

import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.GestoPagoTokenNoDisponibleException;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

/**
 * Configuracion exclusiva de GestoPagoSenTxClient (sendTx.do/confirmTx.do).
 * Igual que GestoPagoProductListFeignConfig, reusa el token que ya obtiene y
 * renueva GestoPagoTokenServiceImpl: sendTx/confirmTx pertenecen al mismo
 * servicio externo (/sistema/app/jwt-gp/authenticate/) que ya autentica el
 * catalogo de productos, con las mismas credenciales de distribuidor.
 */
public class GestoPagoSenTxFeignConfig {

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    @Value("${gestopago.sendtx.api-key:}")
    private String apiKey;

    private final GestoPagoTokenService gestoPagoTokenService;

    public GestoPagoSenTxFeignConfig(GestoPagoTokenService gestoPagoTokenService) {
        this.gestoPagoTokenService = gestoPagoTokenService;
    }

    @Bean
    public RequestInterceptor gestoPagoSenTxAuthInterceptor() {
        return requestTemplate -> {
            requestTemplate.header(HttpHeaders.AUTHORIZATION, "Bearer " + obtenerTokenVigente());
            if (StringUtils.hasText(apiKey)) {
                requestTemplate.header("X-API-Key", apiKey);
            }
        };
    }

    private String obtenerTokenVigente() {
        return gestoPagoTokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                .map(GestoPagoToken::getToken)
                .orElseThrow(() -> new GestoPagoTokenNoDisponibleException(
                        "No hay un token GestoPago activo para idDistribuidor=" + idDistribuidor));
    }
}
