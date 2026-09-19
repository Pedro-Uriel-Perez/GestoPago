package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductListClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.exception.GestoPagoTokenNoDisponibleException;
import com.proyecto.servicios.mapper.ProductoMapper;
import com.proyecto.servicios.model.productlist.MensajeExternoDTO;
import com.proyecto.servicios.model.productlist.ProductListApiResponse;
import com.proyecto.servicios.model.productlist.ProductoExternoDTO;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestoPagoProductListSyncServiceImplTest {

    @Mock
    private GestoPagoProductListClient productListClient;

    @Mock
    private GestoPagoProductoRepository productoRepository;

    @Mock
    private ProductoMapper productoMapper;

    @InjectMocks
    private GestoPagoProductListSyncServiceImpl syncService;

    @Test
    void sincronizarProductos_respuestaExitosa_reemplazaElCatalogoEnPostgres() {
        ProductoExternoDTO externo = new ProductoExternoDTO();
        externo.setIdProducto(1);
        externo.setProducto("Agua Cancun");

        ProductListApiResponse apiResponse = new ProductListApiResponse();
        apiResponse.setMensaje(mensaje("01", "Operacion realizada con exito"));
        apiResponse.setProductos(List.of(externo));

        GestoPagoProducto entidad = new GestoPagoProducto();
        entidad.setIdProducto(1);

        when(productListClient.getProductList()).thenReturn(apiResponse);
        when(productoMapper.toEntityList(apiResponse.getProductos())).thenReturn(List.of(entidad));

        syncService.sincronizarProductos();

        verify(productoRepository, times(1)).deleteAllInBatch();
        verify(productoRepository, times(1)).saveAll(List.of(entidad));
    }

    @Test
    void sincronizarProductos_respuestaConCodigoDeError_noTocaElCatalogoEnPostgres() {
        ProductListApiResponse apiResponse = new ProductListApiResponse();
        apiResponse.setMensaje(mensaje("99", "Distribuidor no autorizado"));

        when(productListClient.getProductList()).thenReturn(apiResponse);

        syncService.sincronizarProductos();

        verify(productoRepository, never()).deleteAllInBatch();
        verify(productoRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void sincronizarProductos_sinTokenGestoPagoDisponible_noTocaElCatalogoEnPostgres() {
        when(productListClient.getProductList())
                .thenThrow(new GestoPagoTokenNoDisponibleException("No hay un token GestoPago activo"));

        syncService.sincronizarProductos();

        verify(productoRepository, never()).deleteAllInBatch();
    }

    @Test
    void sincronizarProductos_errorDeAutenticacion_noTocaElCatalogoEnPostgres() {
        when(productListClient.getProductList()).thenThrow(feignErrorStatus(401));

        syncService.sincronizarProductos();

        verify(productoRepository, never()).deleteAllInBatch();
    }

    @Test
    void sincronizarProductos_timeout_noTocaElCatalogoEnPostgres() {
        when(productListClient.getProductList()).thenThrow(retryableTimeout());

        syncService.sincronizarProductos();

        verify(productoRepository, never()).deleteAllInBatch();
    }

    @Test
    void sincronizarProductos_errorHttpNoAutenticacion_noTocaElCatalogoEnPostgres() {
        when(productListClient.getProductList()).thenThrow(feignErrorStatus(500));

        syncService.sincronizarProductos();

        verify(productoRepository, never()).deleteAllInBatch();
    }

    @Test
    void sincronizarProductos_errorInesperado_noTocaElCatalogoEnPostgres() {
        when(productListClient.getProductList()).thenThrow(new RuntimeException("fallo de red"));

        syncService.sincronizarProductos();

        verify(productoRepository, never()).deleteAllInBatch();
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
