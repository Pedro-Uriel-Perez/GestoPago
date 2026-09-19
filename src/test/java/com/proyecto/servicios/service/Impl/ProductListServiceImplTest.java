package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.mapper.ProductoMapper;
import com.proyecto.servicios.model.productlist.ProductoResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductListServiceImplTest {

    @Mock
    private GestoPagoProductoRepository productoRepository;

    @Mock
    private ProductoMapper productoMapper;

    @InjectMocks
    private ProductListServiceImpl productListService;

    @Test
    void obtenerListaProductos_conCatalogoSincronizado_retornaListaMapeada() {
        GestoPagoProducto entidad = new GestoPagoProducto();
        entidad.setIdProducto(1);
        entidad.setProducto("Agua Cancun");

        ProductoResponse mapeado = new ProductoResponse();
        mapeado.setIdProducto(1);
        mapeado.setNombre("Agua Cancun");

        when(productoRepository.findAll()).thenReturn(List.of(entidad));
        when(productoMapper.toResponseList(List.of(entidad))).thenReturn(List.of(mapeado));

        List<ProductoResponse> resultado = productListService.obtenerListaProductos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Agua Cancun");
    }

    @Test
    void obtenerListaProductos_sinSincronizarAun_retornaListaVacia() {
        when(productoRepository.findAll()).thenReturn(List.of());
        when(productoMapper.toResponseList(List.of())).thenReturn(List.of());

        List<ProductoResponse> resultado = productListService.obtenerListaProductos();

        assertThat(resultado).isEmpty();
    }
}
