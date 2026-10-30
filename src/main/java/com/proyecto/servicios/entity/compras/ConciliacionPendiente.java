package com.proyecto.servicios.entity.compras;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro durable de una transaccion sendTx/confirmTx cuyo resultado no se
 * pudo determinar (timeout en ambos intentos). No se descuenta saldo ni se
 * reintenta automaticamente: queda aqui para que alguien la revise despues
 * contra el estado real en GestoPago.
 */
@Entity
@Table(name = "conciliaciones_pendientes")
@Getter
@Setter
public class ConciliacionPendiente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cuenta_id", nullable = false)
    private Integer cuentaId;

    @Column(name = "cliente_id", nullable = false)
    private Integer clienteId;

    @Column(name = "upc", nullable = false, columnDefinition = "TEXT")
    private String upc;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "resuelta", nullable = false)
    private Boolean resuelta = false;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @PrePersist
    void alCrear() {
        fechaCreacion = LocalDateTime.now();
    }
}
