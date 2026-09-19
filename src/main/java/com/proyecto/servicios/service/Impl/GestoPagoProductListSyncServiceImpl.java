package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductListClient;
import com.proyecto.servicios.exception.GestoPagoTokenNoDisponibleException;
import com.proyecto.servicios.exception.ProductListAuthenticationException;
import com.proyecto.servicios.exception.ProductListCommunicationException;
import com.proyecto.servicios.exception.ProductListException;
import com.proyecto.servicios.exception.ProductListTimeoutException;
import com.proyecto.servicios.exception.ProductListUnsuccessfulResponseException;
import com.proyecto.servicios.mapper.ProductoMapper;
import com.proyecto.servicios.model.productlist.ProductListApiResponse;
import com.proyecto.servicios.model.productlist.ProductoResponse;
import com.proyecto.servicios.service.GestoPagoProductListSyncService;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * GestoPago solo permite invocar getProductList.do "una vez al dia" y hasta
 * 3 veces en total antes de bloquear la IP (ver especificacion PuntoRed), y
 * prohibe usarlo como fuente directa para el frontend. Por eso esta clase
 * corre en un job programado (una vez al dia) y el resultado se guarda
 * UNICAMENTE en el cache de Redis ("productos"), sin persistir nada en
 * Postgres. GET /productos (ProductListServiceImpl) lee ese mismo cache.
 *
 * @Scheduled y @CachePut estan en el MISMO metodo a proposito: si el
 * @CachePut viviera en un metodo aparte invocado internamente (this.metodo())
 * desde el metodo programado, el proxy de Spring nunca interceptaria esa
 * llamada y el cache nunca se actualizaria (auto-invocacion no pasa por el
 * proxy de AOP).
 */
@Service
@Slf4j
public class GestoPagoProductListSyncServiceImpl implements GestoPagoProductListSyncService {

    private final GestoPagoProductListClient productListClient;
    private final ProductoMapper productoMapper;

    public GestoPagoProductListSyncServiceImpl(GestoPagoProductListClient productListClient,
                                                ProductoMapper productoMapper) {
        this.productListClient = productListClient;
        this.productoMapper = productoMapper;
    }

    @Override
    @Scheduled(cron = "${gestopago.productos.sync-cron:0 0 3 * * *}")
    @CachePut(cacheNames = "productos", unless = "#result == null")
    public List<ProductoResponse> sincronizarProductos() {
        log.info("Iniciando sincronizacion del catalogo de productos GestoPago");
        try {
            List<ProductoResponse> productos = consultarCatalogoVigente();
            log.info("Sincronizacion de catalogo de productos GestoPago finalizada. total={}", productos.size());
            return productos;

        } catch (ProductListException e) {
            log.error("La sincronizacion del catalogo de productos GestoPago fallo, se conserva el cache anterior: {}", e.getMessage());
            return null;
        }
    }

    private List<ProductoResponse> consultarCatalogoVigente() {
        try {
            ProductListApiResponse response = productListClient.getProductList();
            return Optional.ofNullable(response)
                    .filter(r -> r.getMensaje() != null && r.getMensaje().esExitoso())
                    .map(ProductListApiResponse::getProductos)
                    .map(productoMapper::toResponseList)
                    .orElseThrow(() -> new ProductListUnsuccessfulResponseException(
                            "GestoPago respondio sin exito al consultar el catalogo de productos", null));

        } catch (GestoPagoTokenNoDisponibleException e) {
            log.error("No hay token GestoPago disponible para sincronizar el catalogo: {}", e.getMessage());
            throw new ProductListAuthenticationException(
                    "No hay un token GestoPago activo para sincronizar el catalogo de productos", e);

        } catch (FeignException.Unauthorized | FeignException.Forbidden e) {
            log.error("GestoPago rechazo la autenticacion al sincronizar el catalogo: status={}", e.status());
            throw new ProductListAuthenticationException(
                    "No fue posible autenticar con GestoPago para sincronizar el catalogo", e);

        } catch (RetryableException e) {
            log.error("GestoPago no respondio a tiempo al sincronizar el catalogo de productos");
            throw new ProductListTimeoutException(
                    "GestoPago no respondio a tiempo al sincronizar el catalogo de productos", e);

        } catch (FeignException e) {
            log.error("GestoPago respondio con un error al sincronizar el catalogo: status={}", e.status());
            throw new ProductListUnsuccessfulResponseException(
                    "GestoPago respondio con un error al sincronizar el catalogo de productos", e);

        } catch (ProductListException e) {
            throw e;

        } catch (Exception e) {
            log.error("Error de comunicacion con GestoPago al sincronizar el catalogo: {}", e.getMessage());
            throw new ProductListCommunicationException(
                    "No fue posible comunicarse con GestoPago para sincronizar el catalogo de productos", e);
        }
    }
}
