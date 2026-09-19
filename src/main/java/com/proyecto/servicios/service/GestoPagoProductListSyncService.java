package com.proyecto.servicios.service;

import com.proyecto.servicios.model.productlist.ProductoResponse;

import java.util.List;

/**
 * Sincroniza el cache de productos/servicios (Redis) con el catalogo que
 * expone GestoPago. GestoPago solo permite invocar getProductList.do hasta
 * 3 veces al dia y prohibe usarlo como fuente directa para el frontend, por
 * lo que esta sincronizacion corre en un job programado (ver implementacion)
 * y el resto de la aplicacion (GET /productos) lee siempre del cache.
 *
 * No se persiste en Postgres: el catalogo vive unicamente en Redis.
 */
public interface GestoPagoProductListSyncService {

    List<ProductoResponse> sincronizarProductos();
}
