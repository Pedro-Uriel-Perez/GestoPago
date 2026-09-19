package com.proyecto.servicios.client;

import com.proyecto.servicios.config.GestoPagoProductListFeignConfig;
import com.proyecto.servicios.model.productlist.ProductListApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "gestoPagoProductList",
        url = "${gestopago.auth.url}",
        configuration = GestoPagoProductListFeignConfig.class
)
public interface GestoPagoProductListClient {

    @GetMapping(value = "/sistema/service/getProductList.do", produces = MediaType.APPLICATION_XML_VALUE)
    ProductListApiResponse getProductList();
}
