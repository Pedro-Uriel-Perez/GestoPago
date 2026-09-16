package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductListClient;
import com.proyecto.servicios.exception.GestoPagoTokenNoDisponibleException;
import com.proyecto.servicios.exception.ProductListAuthenticationException;
import com.proyecto.servicios.exception.ProductListCommunicationException;
import com.proyecto.servicios.exception.ProductListTimeoutException;
import com.proyecto.servicios.exception.ProductListUnsuccessfulResponseException;
import com.proyecto.servicios.mapper.ProductoMapper;
import com.proyecto.servicios.model.productlist.ProductListApiResponse;
import com.proyecto.servicios.model.productlist.ProductoResponse;
import com.proyecto.servicios.service.ProductListService;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ProductListServiceImpl implements ProductListService {

    private final GestoPagoProductListClient productListClient;
    private final ProductoMapper productoMapper;

    public ProductListServiceImpl(GestoPagoProductListClient productListClient, ProductoMapper productoMapper) {
        this.productListClient = productListClient;
        this.productoMapper = productoMapper;
    }

    @Override
    public List<ProductoResponse> obtenerListaProductos() {
        log.info("Iniciando consulta de lista de productos al servicio externo");

        List<ProductoResponse> productos = consultarProductos();

        log.info("Consulta de lista de productos finalizada. total={}", productos.size());
        return productos;
    }

    private List<ProductoResponse> consultarProductos() {
        try {
            ProductListApiResponse response = productListClient.getProductList();
            return Optional.ofNullable(response)
                    .map(ProductListApiResponse::getProductos)
                    .map(productoMapper::toResponseList)
                    .orElseGet(List::of);

        } catch (GestoPagoTokenNoDisponibleException e) {
            log.error("No hay token GestoPago disponible para consultar la lista de productos: {}", e.getMessage());
            throw new ProductListAuthenticationException(
                    "No hay un token GestoPago activo para consultar la lista de productos", e);

        } catch (FeignException.Unauthorized | FeignException.Forbidden e) {
            log.error("El servicio externo de productos rechazo la autenticacion: status={}", e.status());
            throw new ProductListAuthenticationException(
                    "No fue posible autenticar con el servicio de lista de productos", e);

        } catch (RetryableException e) {
            log.error("El servicio externo de productos no respondio a tiempo");
            throw new ProductListTimeoutException(
                    "El servicio de lista de productos no respondio a tiempo", e);

        } catch (FeignException e) {
            log.error("El servicio externo de productos respondio con un error: status={}", e.status());
            throw new ProductListUnsuccessfulResponseException(
                    "El servicio de lista de productos respondio con un error", e);

        } catch (Exception e) {
            log.error("Error de comunicacion con el servicio externo de productos: {}", e.getMessage());
            throw new ProductListCommunicationException(
                    "No fue posible comunicarse con el servicio de lista de productos", e);
        }
    }
}
