package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.model.productlist.ProductoResponse;
import com.proyecto.servicios.service.ProductListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Sirve el catalogo de productos GestoPago unicamente desde el cache de
 * Redis ("productos"): nunca llama a GestoPago en el momento de la peticion
 * (el proveedor solo permite consultar getProductList.do hasta 3 veces al
 * dia y prohibe usarlo como fuente directa para el frontend) y no persiste
 * nada en Postgres. GestoPagoProductListSyncServiceImpl es quien llena ese
 * cache una vez al dia (@CachePut, misma cache name/clave por defecto).
 *
 * Si todavia no ha corrido ninguna sincronizacion, el cache esta vacio y
 * este metodo se ejecuta una vez, cachea y retorna una lista vacia.
 */
@Service
@Slf4j
public class ProductListServiceImpl implements ProductListService {

    @Override
    @Cacheable(cacheNames = "productos")
    public List<ProductoResponse> obtenerListaProductos() {
        log.info("No hay catalogo de productos GestoPago en cache todavia; se retorna lista vacia");
        return List.of();
    }
}
