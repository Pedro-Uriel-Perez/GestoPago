package com.proyecto.servicios.config;

import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.service.GestoPagoProductListSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * El catalogo de productos normalmente se llena con el job programado
 * (GestoPagoProductListSyncServiceImpl, una vez al dia a las 6 AM). En un
 * despliegue nuevo (ej. Render con una base de datos recien creada) esa
 * tabla arranca vacia, y en el plan gratis de Render la app se duerme por
 * inactividad: si nadie la visita justo a las 6 AM, el cron nunca dispara y
 * el catalogo se queda vacio para siempre.
 *
 * Por eso, al arrancar, si la tabla esta vacia se sincroniza una sola vez
 * aqui. Una vez que la sincronizacion tiene exito, la tabla deja de estar
 * vacia y esto no se vuelve a disparar en ningun arranque futuro — sigue
 * respetando el limite de GestoPago de 3 llamadas/dia a getProductList.do.
 *
 * Se llama a traves de la interfaz (bean administrado por Spring), no desde
 * dentro de GestoPagoProductListSyncServiceImpl, para que @CacheEvict en
 * sincronizarProductos() pase por el proxy de AOP correctamente.
 */
@Component
@Slf4j
public class GestoPagoProductosArranque implements ApplicationRunner {

    private final GestoPagoProductoRepository productoRepository;
    private final GestoPagoProductListSyncService syncService;

    public GestoPagoProductosArranque(GestoPagoProductoRepository productoRepository,
                                       GestoPagoProductListSyncService syncService) {
        this.productoRepository = productoRepository;
        this.syncService = syncService;
    }

    @Override
    public void run(ApplicationArguments args) {
        Optional.of(productoRepository.count())
                .filter(total -> total == 0)
                .ifPresent(total -> {
                    log.info("Catalogo de productos vacio al arrancar, sincronizando una vez con GestoPago");
                    syncService.sincronizarProductos();
                });
    }
}
