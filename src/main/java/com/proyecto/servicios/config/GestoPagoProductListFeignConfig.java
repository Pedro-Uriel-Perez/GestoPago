package com.proyecto.servicios.config;

import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.GestoPagoTokenNoDisponibleException;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;

/**
 * Configuracion exclusiva del GestoPagoProductListClient. Se referencia via
 * {@code @FeignClient(configuration = GestoPagoProductListFeignConfig.class)} y por
 * eso NO lleva {@code @Configuration}: Spring Cloud OpenFeign la registra en
 * un contexto hijo propio del cliente; si tuviera {@code @Configuration}
 * quedaria tambien en el escaneo global y el interceptor se aplicaria a
 * todos los Feign clients de la aplicacion (incluido GestoPagoAuthClient).
 *
 * El Bearer Token no se lee de una propiedad estatica: se reutiliza el mismo
 * token GestoPago que ya obtiene y renueva GestoPagoTokenServiceImpl, ya que
 * ambos endpoints (autenticacion y lista de productos) pertenecen al mismo
 * servicio externo.
 */
public class GestoPagoProductListFeignConfig {

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    private final GestoPagoTokenService gestoPagoTokenService;

    public GestoPagoProductListFeignConfig(GestoPagoTokenService gestoPagoTokenService) {
        this.gestoPagoTokenService = gestoPagoTokenService;
    }

    @Bean
    public RequestInterceptor gestoPagoProductListAuthInterceptor() {
        return requestTemplate -> requestTemplate.header(HttpHeaders.AUTHORIZATION, "Bearer " + obtenerTokenVigente());
    }

    private String obtenerTokenVigente() {
        return gestoPagoTokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                .map(GestoPagoToken::getToken)
                .orElseThrow(() -> new GestoPagoTokenNoDisponibleException(
                        "No hay un token GestoPago activo para idDistribuidor=" + idDistribuidor));
    }
}
