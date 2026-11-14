package com.proyecto.servicios.entity.compras;

import com.proyecto.servicios.config.ZonaHoraria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Una peticion a GestoPago con su respuesta (ver V15__create_gestopago_bitacora.sql). */
@Entity
@Table(name = "gestopago_bitacora")
@Getter
@Setter
public class BitacoraGestoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "operacion", nullable = false, length = 30)
    private String operacion;

    @Column(name = "upc", length = 40)
    private String upc;

    @Column(name = "peticion", columnDefinition = "TEXT")
    private String peticion;

    @Column(name = "respuesta", columnDefinition = "TEXT")
    private String respuesta;

    @Column(name = "codigo", length = 10)
    private String codigo;

    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    @Column(name = "duracion_ms")
    private Long duracionMs;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    @PrePersist
    void alCrear() {
        fecha = LocalDateTime.now(ZonaHoraria.MEXICO);
    }
}
