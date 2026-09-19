package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductListClient;
import com.proyecto.servicios.exception.GestoPagoTokenNoDisponibleException;
import com.proyecto.servicios.mapper.ProductoMapper;
import com.proyecto.servicios.model.productlist.MensajeExternoDTO;
import com.proyecto.servicios.model.productlist.ProductListApiResponse;
import com.proyecto.servicios.model.productlist.ProductoExternoDTO;
import com.proyecto.servicios.model.productlist.ProductoResponse;
import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Estas pruebas invocan sincronizarProductos() directamente sobre el objeto
 * (sin contexto de Spring), por lo que @Scheduled y @CachePut no se activan
 * (son AOP de Spring) - se verifica unicamente el valor que el metodo
 * retorna: la lista mapeada en exito, o null en cada escenario de error
 * (que es justo lo que hace que @CachePut, en tiempo de ejecucion real, NO
 * sobreescriba el cache existente).
 */
@ExtendWith(MockitoExtension.class)
class GestoPagoProductListSyncServiceImplTest {

    @Mock
    private GestoPagoProductListClient productListClient;

    @Mock
    private ProductoMapper productoMapper;

    @InjectMocks
    private GestoPagoProductListSyncServiceImpl syncService;

    @Test
    void sincronizarProductos_respuestaExitosa_retornaListaMapeada() {
        ProductoExternoDTO externo = new ProductoExternoDTO();
        externo.setIdProducto(1);
        externo.setProducto("Agua Cancun");

        ProductListApiResponse apiResponse = new ProductListApiResponse();
        apiResponse.setMensaje(mensaje("01", "Operacion realizada con exito"));
        apiResponse.setProductos(List.of(externo));

        ProductoResponse mapeado = new ProductoResponse();
        mapeado.setIdProducto(1);
        mapeado.setNombre("Agua Cancun");

        when(productListClient.getProductList()).thenReturn(apiResponse);
        when(productoMapper.toResponseList(apiResponse.getProductos())).thenReturn(List.of(mapeado));

        List<ProductoResponse> resultado = syncService.sincronizarProductos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Agua Cancun");
    }

    @Test
    void sincronizarProductos_respuestaConCodigoDeError_retornaNull() {
        ProductListApiResponse apiResponse = new ProductListApiResponse();
        apiResponse.setMensaje(mensaje("99", "Distribuidor no autorizado"));

        when(productListClient.getProductList()).thenReturn(apiResponse);

        assertThat(syncService.sincronizarProductos()).isNull();
    }

    @Test
    void sincronizarProductos_sinTokenGestoPagoDisponible_retornaNull() {
        when(productListClient.getProductList())
                .thenThrow(new GestoPagoTokenNoDisponibleException("No hay un token GestoPago activo"));

        assertThat(syncService.sincronizarProductos()).isNull();
    }

    @Test
    void sincronizarProductos_errorDeAutenticacion_retornaNull() {
        when(productListClient.getProductList()).thenThrow(feignErrorStatus(401));

        assertThat(syncService.sincronizarProductos()).isNull();
    }

    @Test
    void sincronizarProductos_timeout_retornaNull() {
        when(productListClient.getProductList()).thenThrow(retryableTimeout());

        assertThat(syncService.sincronizarProductos()).isNull();
    }

    @Test
    void sincronizarProductos_errorHttpNoAutenticacion_retornaNull() {
        when(productListClient.getProductList()).thenThrow(feignErrorStatus(500));

        assertThat(syncService.sincronizarProductos()).isNull();
    }

    @Test
    void sincronizarProductos_errorInesperado_retornaNull() {
        when(productListClient.getProductList()).thenThrow(new RuntimeException("fallo de red"));

        assertThat(syncService.sincronizarProductos()).isNull();
    }

    private MensajeExternoDTO mensaje(String codigo, String texto) {
        MensajeExternoDTO mensaje = new MensajeExternoDTO();
        mensaje.setCodigo(codigo);
        mensaje.setTexto(texto);
        return mensaje;
    }

    private FeignException feignErrorStatus(int status) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/sistema/service/getProductList.do",
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8,
                null);
        Response response = Response.builder()
                .status(status)
                .reason("error")
                .request(request)
                .headers(Collections.emptyMap())
                .build();
        return FeignException.errorStatus("GestoPagoProductListClient#getProductList()", response);
    }

    private RetryableException retryableTimeout() {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/sistema/service/getProductList.do",
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8,
                null);
        return new RetryableException(
                -1,
                "Read timed out",
                Request.HttpMethod.GET,
                (Long) null,
                request);
    }
}
