package com.proyecto.servicios.config;

import java.time.ZoneId;

/**
 * Zona horaria de negocio. El servidor de produccion (Render) corre en UTC,
 * asi que LocalDateTime.now() sin zona daria la hora de UTC, no la de
 * Mexico; por ejemplo, la "horaLocal" que exige GestoPago saldria 6 horas
 * adelantada.
 */
public final class ZonaHoraria {

    public static final ZoneId MEXICO = ZoneId.of("America/Mexico_City");

    private ZonaHoraria() {
    }
}
