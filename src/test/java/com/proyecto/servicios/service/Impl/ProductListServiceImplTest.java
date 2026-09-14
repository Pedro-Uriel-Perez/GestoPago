package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.ProductListClient;
import com.proyecto.servicios.exception.ProductListAuthenticationException;
import com.proyecto.servicios.exception.ProductListCommunicationException;
import com.proyecto.servicios.exception.ProductListTimeoutException;
import com.proyecto.servicios.exception.ProductListUnsuccessfulResponseException;
import com.proyecto.servicios.mapper.ProductoMapper;
import com.proyecto.servicios.model.productlist.ProductListApiResponse;
import com.proyecto.servicios.model.productlist.ProductoExternoDTO;
import com.proyecto.servicios.model.productlist.ProductoResponse;
import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductListServiceImplTest {

    @Mock
    private ProductListClient productListClient;

    @Mock
    private ProductoMapper productoMapper;

    @InjectMocks
    private ProductListServiceImpl productListService;

    @Test
    void obtenerListaProductos_respuestaExitosa_retornaListaMapeada() {
        ProductoExternoDTO externo = new ProductoExternoDTO();
        externo.setCodigo("P1");
        externo.setNombre("Producto 1");
        externo.setPrecio(BigDecimal.TEN);
        externo.setExistencia(5);

        ProductListApiResponse apiResponse = new ProductListApiResponse();
        apiResponse.setProductos(List.of(externo));

        ProductoResponse mapeado = new ProductoResponse();
        mapeado.setCodigo("P1");
        mapeado.setNombre("Producto 1");

        when(productListClient.getProductList()).thenReturn(apiResponse);
        when(productoMapper.toResponseList(apiResponse.getProductos())).thenReturn(List.of(mapeado));

        List<ProductoResponse> resultado = productListService.obtenerListaProductos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCodigo()).isEqualTo("P1");
    }

    @Test
    void obtenerListaProductos_respuestaSinProductos_retornaListaVacia() {
        ProductListApiResponse apiResponse = new ProductListApiResponse();
        apiResponse.setProductos(null);

        when(productListClient.getProductList()).thenReturn(apiResponse);

        List<ProductoResponse> resultado = productListService.obtenerListaProductos();

        assertThat(resultado).isEmpty();
    }

    @Test
    void obtenerListaProductos_errorDeAutenticacion_lanzaProductListAuthenticationException() {
        when(productListClient.getProductList()).thenThrow(feignErrorStatus(401));

        assertThatThrownBy(() -> productListService.obtenerListaProductos())
                .isInstanceOf(ProductListAuthenticationException.class);
    }

    @Test
    void obtenerListaProductos_timeout_lanzaProductListTimeoutException() {
        when(productListClient.getProductList()).thenThrow(retryableTimeout());

        assertThatThrownBy(() -> productListService.obtenerListaProductos())
                .isInstanceOf(ProductListTimeoutException.class);
    }

    @Test
    void obtenerListaProductos_errorHttpNoAutenticacion_lanzaProductListUnsuccessfulResponseException() {
        when(productListClient.getProductList()).thenThrow(feignErrorStatus(500));

        assertThatThrownBy(() -> productListService.obtenerListaProductos())
                .isInstanceOf(ProductListUnsuccessfulResponseException.class);
    }

    @Test
    void obtenerListaProductos_errorInesperado_lanzaProductListCommunicationException() {
        when(productListClient.getProductList()).thenThrow(new RuntimeException("fallo de red"));

        assertThatThrownBy(() -> productListService.obtenerListaProductos())
                .isInstanceOf(ProductListCommunicationException.class);
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
        return FeignException.errorStatus("ProductListClient#getProductList()", response);
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
