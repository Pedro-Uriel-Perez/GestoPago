package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.GestoPagoCatalogo;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.mapper.ProductoMapper;
import com.proyecto.servicios.model.productlist.ProductoResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.service.ProductListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    private static final int TIPO_FRONT_CONSULTA = 4;

    private final GestoPagoProductoRepository productoRepository;
    private final ProductoMapper productoMapper;
    private final GestoPagoCatalogo catalogo;

    public ProductListServiceImpl(GestoPagoProductoRepository productoRepository,
                                   ProductoMapper productoMapper,
                                   GestoPagoCatalogo catalogo) {
        this.productoRepository = productoRepository;
        this.productoMapper = productoMapper;
        this.catalogo = catalogo;
    }

    @Override
    @Cacheable(cacheNames = "catalogo")
    public List<ProductoResponse> obtenerListaProductos() {
        log.info("Consultando catalogo de productos GestoPago en Postgres (cache-miss de Redis)");

        List<GestoPagoProducto> entidades = productoRepository.findAll();

        // Los productos tipoFront 4 son la "consulta de saldo" de un
        // servicio: si un servicio tiene uno, su referencia se puede validar.
        Set<Integer> serviciosConConsulta = entidades.stream()
                .filter(producto -> Integer.valueOf(TIPO_FRONT_CONSULTA).equals(producto.getTipoFront()))
                .map(GestoPagoProducto::getIdServicio)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<ProductoResponse> productos = productoMapper.toResponseList(entidades);
        productos.forEach(producto -> {
            producto.setCategoria(catalogo.categoria(producto.getIdCatTipoServicio()));
            producto.setLogoUrl(catalogo.logoUrl(producto.getIdServicio()));
            producto.setImagenReferenciaUrl(catalogo.imagenReferenciaUrl(producto.getIdServicio()));
            producto.setPermiteValidarReferencia(serviciosConConsulta.contains(producto.getIdServicio()));
        });

        log.info("Catalogo de productos GestoPago consultado. total={}", productos.size());
        return productos;
    }
}
