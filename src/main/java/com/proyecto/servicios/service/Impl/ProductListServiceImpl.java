package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.mapper.ProductoMapper;
import com.proyecto.servicios.model.productlist.ProductoResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.service.ProductListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Sirve el catalogo de productos GestoPago desde la copia local
 * (gestopago_productos), nunca llamando a GestoPago en el momento de la
 * peticion: el proveedor solo permite consultar getProductList.do hasta 3
 * veces al dia y prohibe usarlo como fuente directa para el frontend (ver
 * GestoPagoProductListSyncServiceImpl, que mantiene esta copia actualizada).
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
    public List<ProductoResponse> obtenerListaProductos() {
        log.info("Consultando catalogo local de productos GestoPago");

        List<ProductoResponse> productos = productoMapper.toResponseList(productoRepository.findAll());

        log.info("Catalogo local de productos GestoPago consultado. total={}", productos.size());
        return productos;
    }
}
