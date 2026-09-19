package com.proyecto.servicios.service;

/**
 * Sincroniza el catalogo local de productos/servicios con el que expone
 * GestoPago. GestoPago solo permite invocar getProductList.do hasta 3 veces
 * al dia y prohibe usarlo como fuente directa para el frontend, por lo que
 * esta sincronizacion corre en un job programado (ver implementacion) y el
 * resto de la aplicacion (GET /productos) lee siempre del catalogo local.
 */
public interface GestoPagoProductListSyncService {

    void sincronizarProductos();
}
