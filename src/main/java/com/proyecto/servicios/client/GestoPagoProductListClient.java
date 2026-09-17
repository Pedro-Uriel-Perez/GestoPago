package com.proyecto.servicios.client;

import com.proyecto.servicios.config.GestoPagoProductListFeignConfig;
import com.proyecto.servicios.model.productlist.ProductListApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "gestoPagoProductList",
        url = "${gestopago.auth.url}",
        configuration = GestoPagoProductListFeignConfig.class
)
public interface GestoPagoProductListClient {

    @GetMapping("/sistema/service/getProductList.do")
    ProductListApiResponse getProductList();
}
