package com.proyecto.servicios.config;

import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.service.GestoPagoProductListSyncService;
import com.proyecto.servicios.service.GestoPagoTokenService;
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
    private final GestoPagoTokenService tokenService;

    public GestoPagoProductosArranque(GestoPagoProductoRepository productoRepository,
                                       GestoPagoProductListSyncService syncService,
                                       GestoPagoTokenService tokenService) {
        this.productoRepository = productoRepository;
        this.syncService = syncService;
        this.tokenService = tokenService;
    }

    @Override
    public void run(ApplicationArguments args) {
        Optional.of(productoRepository.count())
                .filter(total -> total == 0)
                .ifPresent(total -> {
                    log.info("Catalogo de productos vacio al arrancar, sincronizando una vez con GestoPago");
                    // En una base de datos nueva tampoco hay token guardado y
                    // la tarea programada que lo obtiene corre en otro hilo:
                    // sin esto la sincronizacion se adelantaba al token y
                    // fallaba, dejando el catalogo vacio.
                    tokenService.renovarToken();
                    syncService.sincronizarProductos();
                });
    }
}
