package com.proyecto.servicios.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;

/**
 * Configuracion exclusiva del ProductListClient. Se referencia via
 * {@code @FeignClient(configuration = ProductListFeignConfig.class)} y por
 * eso NO lleva {@code @Configuration}: Spring Cloud OpenFeign la registra en
 * un contexto hijo propio del cliente; si tuviera {@code @Configuration}
 * quedaria tambien en el escaneo global y el interceptor se aplicaria a
 * todos los Feign clients de la aplicacion (incluido GestoPagoAuthClient).
 */
public class ProductListFeignConfig {

    @Value("${productlist.api.token}")
    private String apiToken;

    @Bean
    public RequestInterceptor productListAuthInterceptor() {
        return requestTemplate -> requestTemplate.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken);
    }
}
