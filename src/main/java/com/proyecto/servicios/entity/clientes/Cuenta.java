package com.proyecto.servicios.entity.clientes;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "cuentas")
@Getter
@Setter
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    private Cliente cliente;

    @Column(name = "numero_cuenta", nullable = false, unique = true, columnDefinition = "TEXT")
    private String numeroCuenta;

    @Column(name = "estatus", nullable = false, columnDefinition = "TEXT")
    private String estatus = "ACTIVA";

    @Column(name = "fecha_apertura", nullable = false, updatable = false)
    private LocalDateTime fechaApertura;

    @PrePersist
    void alCrear() {
        fechaApertura = LocalDateTime.now();
    }
}
