package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.model.productlist.ProductoResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductListServiceImplTest {

    private final ProductListServiceImpl productListService = new ProductListServiceImpl();

    @Test
    void obtenerListaProductos_sinCacheAun_retornaListaVacia() {
        List<ProductoResponse> resultado = productListService.obtenerListaProductos();

        assertThat(resultado).isEmpty();
    }
}
