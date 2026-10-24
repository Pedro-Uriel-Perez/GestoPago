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
}
