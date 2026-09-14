package com.proyecto.servicios.client;

import com.proyecto.servicios.config.ProductListFeignConfig;
import com.proyecto.servicios.model.productlist.ProductListApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "productListClient",
        url = "${productlist.api.url}",
        configuration = ProductListFeignConfig.class
)
public interface ProductListClient {

    @GetMapping("/sistema/service/getProductList.do")
    ProductListApiResponse getProductList();
}
