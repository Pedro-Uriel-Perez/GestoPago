package com.proyecto.servicios.entity.gestopago;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class GestoPagoTokenTest {

    @Test
    void nuevoToken_estaActivoPorDefecto() {
        assertThat(new GestoPagoToken().getActivo()).isTrue();
    }

    @Test
    void onCreate_asignaFechaDeCreacionYActualizacion() {
        GestoPagoToken token = new GestoPagoToken();

        token.onCreate();

        assertThat(token.getFechaCreacion()).isNotNull();
        assertThat(token.getFechaActualizacion()).isNotNull();
    }

    @Test
    void onUpdate_soloCambiaLaFechaDeActualizacion() {
        LocalDateTime creacion = LocalDateTime.of(2026, 1, 1, 8, 0);
        GestoPagoToken token = new GestoPagoToken();
        token.setFechaCreacion(creacion);
        token.setFechaActualizacion(creacion);

        token.onUpdate();

        assertThat(token.getFechaCreacion()).isEqualTo(creacion);
        assertThat(token.getFechaActualizacion()).isAfter(creacion);
    }
}
