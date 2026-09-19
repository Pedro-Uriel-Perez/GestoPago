package com.proyecto.servicios.service;

/**
 * Sincroniza el catalogo local de productos/servicios (Postgres, tabla
 * gestopago_productos) con el que expone GestoPago, e invalida el cache de
 * Redis ("productos") para que la siguiente lectura recargue datos frescos.
 * GestoPago solo permite invocar getProductList.do hasta 3 veces al dia y
 * prohibe usarlo como fuente directa para el frontend, por lo que esta
 * sincronizacion corre en un job programado (ver implementacion) y el resto
 * de la aplicacion (GET /productos) nunca llama a GestoPago directamente.
 */
public interface GestoPagoProductListSyncService {

    void sincronizarProductos();
}
