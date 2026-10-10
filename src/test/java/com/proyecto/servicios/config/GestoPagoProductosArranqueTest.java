package com.proyecto.servicios.config;

import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.service.GestoPagoProductListSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestoPagoProductosArranqueTest {

    @Mock
    private GestoPagoProductoRepository productoRepository;

    @Mock
    private GestoPagoProductListSyncService syncService;

    @InjectMocks
    private GestoPagoProductosArranque arranque;

    @Test
    void run_catalogoVacio_disparaUnaSincronizacion() throws Exception {
        when(productoRepository.count()).thenReturn(0L);

        arranque.run(null);

        verify(syncService).sincronizarProductos();
    }

    @Test
    void run_catalogoConDatos_noDisparaSincronizacion() throws Exception {
        when(productoRepository.count()).thenReturn(900L);

        arranque.run(null);

        verify(syncService, never()).sincronizarProductos();
    }
}
