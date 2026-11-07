package com.proyecto.servicios.service.Impl;

import org.springframework.http.HttpStatus;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.client.GestoPagoProductListClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.mapper.ProductoMapper;
import com.proyecto.servicios.model.productlist.ProductListApiResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.service.GestoPagoProductListSyncService;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * GestoPago solo permite invocar getProductList.do "una vez al dia" y hasta
 * 3 veces en total antes de bloquear la IP (ver especificacion PuntoRed), y
 * prohibe usarlo como fuente directa para el frontend. Por eso esta clase
 * corre en un job programado (una vez al dia), guarda el resultado en
 * Postgres (gestopago_productos, fuente de verdad/respaldo) e invalida el
 * cache de Redis para forzar una recarga fresca en la siguiente lectura.
 *
 * @Scheduled y @CacheEvict estan en el MISMO metodo a proposito: si el
 * @CacheEvict viviera en un metodo aparte invocado internamente
 * (this.metodo()) desde el metodo programado, el proxy de Spring nunca
 * interceptaria esa llamada y el cache jamas se invalidaria
 * (auto-invocacion no pasa por el proxy de AOP).
 */
@Service
@Slf4j
public class GestoPagoProductListSyncServiceImpl implements GestoPagoProductListSyncService {

    private final GestoPagoProductListClient productListClient;
    private final GestoPagoProductoRepository productoRepository;
    private final ProductoMapper productoMapper;

    public GestoPagoProductListSyncServiceImpl(GestoPagoProductListClient productListClient,
                                                GestoPagoProductoRepository productoRepository,
                                                ProductoMapper productoMapper) {
        this.productListClient = productListClient;
        this.productoRepository = productoRepository;
        this.productoMapper = productoMapper;
    }

    @Override
    @Scheduled(cron = "${gestopago.productos.sync-cron:0 0 3 * * *}")
    @Transactional
    @CacheEvict(cacheNames = "productos", allEntries = true)
    public void sincronizarProductos() {
        log.info("Iniciando sincronizacion del catalogo de productos GestoPago");
        try {
            List<GestoPagoProducto> productos = consultarCatalogoVigente();
            productoRepository.deleteAllInBatch();
            productoRepository.saveAll(productos);
            log.info("Sincronizacion de catalogo de productos GestoPago finalizada. total={}", productos.size());

        } catch (NegocioException e) {
            log.error("La sincronizacion del catalogo de productos GestoPago fallo, se conserva el catalogo anterior en Postgres: {}", e.getMessage());
        }
    }

    private List<GestoPagoProducto> consultarCatalogoVigente() {
        try {
            ProductListApiResponse response = productListClient.getProductList();
            return Optional.ofNullable(response)
                    .filter(r -> r.getMensaje() != null && r.getMensaje().esExitoso())
                    .map(ProductListApiResponse::getProductos)
                    .map(productoMapper::toEntityList)
                    .orElseThrow(() -> new NegocioException(HttpStatus.BAD_GATEWAY,
                            "GestoPago respondio sin exito al consultar el catalogo de productos"));

        } catch (NegocioException e) {
            // Ya trae su propio mensaje: respuesta sin exito, o el
            // interceptor de Feign no encontro un token GestoPago vigente.
            throw e;

        } catch (FeignException.Unauthorized | FeignException.Forbidden e) {
            log.error("GestoPago rechazo la autenticacion al sincronizar el catalogo: status={}", e.status());
            throw new NegocioException(HttpStatus.BAD_GATEWAY,
                    "No fue posible autenticar con GestoPago para sincronizar el catalogo", e);

        } catch (RetryableException e) {
            log.error("GestoPago no respondio a tiempo al sincronizar el catalogo de productos");
            throw new NegocioException(HttpStatus.GATEWAY_TIMEOUT,
                    "GestoPago no respondio a tiempo al sincronizar el catalogo de productos", e);

        } catch (FeignException e) {
            log.error("GestoPago respondio con un error al sincronizar el catalogo: status={}", e.status());
            throw new NegocioException(HttpStatus.BAD_GATEWAY,
                    "GestoPago respondio con un error al sincronizar el catalogo de productos", e);

        } catch (Exception e) {
            log.error("Error de comunicacion con GestoPago al sincronizar el catalogo: {}", e.getMessage());
            throw new NegocioException(HttpStatus.BAD_GATEWAY,
                    "No fue posible comunicarse con GestoPago para sincronizar el catalogo de productos", e);
        }
    }
}
