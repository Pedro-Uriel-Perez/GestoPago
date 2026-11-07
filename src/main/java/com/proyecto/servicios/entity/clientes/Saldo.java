package com.proyecto.servicios.entity.clientes;

import com.proyecto.servicios.config.ZonaHoraria;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Historial de movimientos de saldo de una cuenta (1:N). El saldo vigente
 * de una cuenta es el ultimo registro insertado (mayor id), no una columna
 * en Cuenta. Se ordena por id y no por fecha_movimiento para que el orden
 * no dependa del reloj ni de la zona horaria del servidor.
 */
@Entity
@Table(name = "saldos")
@Getter
@Setter
public class Saldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "tipo_movimiento", nullable = false, columnDefinition = "TEXT")
    private String tipoMovimiento;

    @Column(name = "fecha_movimiento", nullable = false, updatable = false)
    private LocalDateTime fechaMovimiento;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @PrePersist
    void alCrear() {
        fechaMovimiento = LocalDateTime.now(ZonaHoraria.MEXICO);
    }
}
