package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.mapper.ProductoMapper;
import com.proyecto.servicios.model.productlist.ProductoResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.service.ProductListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Sirve el catalogo de productos GestoPago desde Postgres
 * (gestopago_productos, fuente de verdad/respaldo), cacheado en Redis
 * ("productos") para que consultas repetidas no vuelvan a golpear la base
 * de datos. Nunca llama a GestoPago en el momento de la peticion (el
 * proveedor solo permite consultar getProductList.do hasta 3 veces al dia y
 * prohibe usarlo como fuente directa para el frontend);
 * GestoPagoProductListSyncServiceImpl mantiene Postgres actualizado una vez
 * al dia e invalida este cache en cada sincronizacion.
 */
@Service
@Slf4j
public class ProductListServiceImpl implements ProductListService {

    private final GestoPagoProductoRepository productoRepository;
    private final ProductoMapper productoMapper;

    public ProductListServiceImpl(GestoPagoProductoRepository productoRepository, ProductoMapper productoMapper) {
        this.productoRepository = productoRepository;
        this.productoMapper = productoMapper;
    }

    @Override
    @Cacheable(cacheNames = "productos")
    public List<ProductoResponse> obtenerListaProductos() {
        log.info("Consultando catalogo de productos GestoPago en Postgres (cache-miss de Redis)");

        List<ProductoResponse> productos = productoMapper.toResponseList(productoRepository.findAll());

        log.info("Catalogo de productos GestoPago consultado. total={}", productos.size());
        return productos;
    }
}
