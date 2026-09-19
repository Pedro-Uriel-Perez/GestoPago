package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductListClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.exception.GestoPagoTokenNoDisponibleException;
import com.proyecto.servicios.exception.ProductListAuthenticationException;
import com.proyecto.servicios.exception.ProductListCommunicationException;
import com.proyecto.servicios.exception.ProductListTimeoutException;
import com.proyecto.servicios.exception.ProductListUnsuccessfulResponseException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void sincronizarProductos_respuestaExitosa_reemplazaElCatalogoLocal() {
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
    void sincronizarProductos_respuestaConCodigoDeError_lanzaProductListUnsuccessfulResponseException() {
        ProductListApiResponse apiResponse = new ProductListApiResponse();
        apiResponse.setMensaje(mensaje("99", "Distribuidor no autorizado"));

        when(productListClient.getProductList()).thenReturn(apiResponse);

        assertThatThrownBy(() -> syncService.sincronizarProductos())
                .isInstanceOf(ProductListUnsuccessfulResponseException.class);
        verify(productoRepository, never()).deleteAllInBatch();
    }

    @Test
    void sincronizarProductos_sinTokenGestoPagoDisponible_lanzaProductListAuthenticationException() {
        when(productListClient.getProductList())
                .thenThrow(new GestoPagoTokenNoDisponibleException("No hay un token GestoPago activo"));

        assertThatThrownBy(() -> syncService.sincronizarProductos())
                .isInstanceOf(ProductListAuthenticationException.class);
    }

    @Test
    void sincronizarProductos_errorDeAutenticacion_lanzaProductListAuthenticationException() {
        when(productListClient.getProductList()).thenThrow(feignErrorStatus(401));

        assertThatThrownBy(() -> syncService.sincronizarProductos())
                .isInstanceOf(ProductListAuthenticationException.class);
    }

    @Test
    void sincronizarProductos_timeout_lanzaProductListTimeoutException() {
        when(productListClient.getProductList()).thenThrow(retryableTimeout());

        assertThatThrownBy(() -> syncService.sincronizarProductos())
                .isInstanceOf(ProductListTimeoutException.class);
    }

    @Test
    void sincronizarProductos_errorHttpNoAutenticacion_lanzaProductListUnsuccessfulResponseException() {
        when(productListClient.getProductList()).thenThrow(feignErrorStatus(500));

        assertThatThrownBy(() -> syncService.sincronizarProductos())
                .isInstanceOf(ProductListUnsuccessfulResponseException.class);
    }

    @Test
    void sincronizarProductos_errorInesperado_lanzaProductListCommunicationException() {
        when(productListClient.getProductList()).thenThrow(new RuntimeException("fallo de red"));

        assertThatThrownBy(() -> syncService.sincronizarProductos())
                .isInstanceOf(ProductListCommunicationException.class);
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
