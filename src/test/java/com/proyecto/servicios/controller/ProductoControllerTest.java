package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.productlist.ProductoListResponse;
import com.proyecto.servicios.model.productlist.ProductoResponse;
import com.proyecto.servicios.service.ProductListService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoControllerTest {

    @Mock
    private ProductListService productListService;

    @InjectMocks
    private ProductoController productoController;

    @Test
    void obtenerProductos_envuelveElCatalogoEnMensajeYData() {
        ProductoResponse producto = new ProductoResponse();
        producto.setIdProducto(345);
        producto.setNombre("Agua Cancun");

        when(productListService.obtenerListaProductos()).thenReturn(List.of(producto));

        ResponseEntity<ProductoListResponse> respuesta = productoController.obtenerProductos();

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().getMensaje()).isEqualTo("Datos consultados correctamente");
        assertThat(respuesta.getBody().getData()).hasSize(1);
        assertThat(respuesta.getBody().getData().get(0).getNombre()).isEqualTo("Agua Cancun");
    }
}
