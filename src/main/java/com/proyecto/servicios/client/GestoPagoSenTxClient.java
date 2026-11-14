package com.proyecto.servicios.client;

import com.proyecto.servicios.config.GestoPagoSenTxFeignConfig;
import com.proyecto.servicios.model.gestopago.GestoPagoSenTxResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "gestoPagoSenTx",
        url = "${gestopago.auth.url}",
        configuration = GestoPagoSenTxFeignConfig.class
)
public interface GestoPagoSenTxClient {

    @PostMapping(value = "/sistema/service/sendTx.do",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    GestoPagoSenTxResponse sendTx(@RequestBody org.springframework.util.MultiValueMap<String, String> form);

    @PostMapping(value = "/sistema/service/confirmTx.do",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    GestoPagoSenTxResponse confirmTx(@RequestBody org.springframework.util.MultiValueMap<String, String> form);

    /**
     * Valida una referencia antes de pagar. Mismo formato (random + signed
     * cifrado) y misma forma de respuesta que sendTx: MENSAJE con CODIGO 01
     * "Referencia Valida" (+ SALDO adeudado) o CODIGO distinto si no lo es.
     */
    @PostMapping(value = "/sistema/service/verifyReference.do",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    GestoPagoSenTxResponse verifyReference(@RequestBody org.springframework.util.MultiValueMap<String, String> form);
}
