package com.proyecto.servicios.entity.clientes;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ClienteTest {

    @Test
    void alCrear_asignaFechaDeRegistroYActualizacion() {
        Cliente cliente = new Cliente();

        cliente.alCrear();

        assertThat(cliente.getFechaRegistro()).isNotNull();
        assertThat(cliente.getFechaActualizacion()).isNotNull();
        assertThat(cliente.getActivo()).isTrue();
    }

    @Test
    void alActualizar_soloCambiaLaFechaDeActualizacion() {
        LocalDateTime registro = LocalDateTime.of(2026, 1, 1, 8, 0);
        Cliente cliente = new Cliente();
        cliente.setFechaRegistro(registro);
        cliente.setFechaActualizacion(registro);

        cliente.alActualizar();

        assertThat(cliente.getFechaRegistro()).isEqualTo(registro);
        assertThat(cliente.getFechaActualizacion()).isAfter(registro);
    }
}
