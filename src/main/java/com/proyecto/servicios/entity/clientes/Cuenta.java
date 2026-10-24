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

    /**
     * EAGER a proposito: los metodos de lectura que resuelven cliente a
     * partir de una cuenta (ej. ClienteServiceImpl.buscarClientes con filtro
     * numeroCuenta, CompraServiceImpl.iniciarCompra) ya no usan
     * @Transactional, asi que no hay sesion de Hibernate abierta para
     * resolver un proxy LAZY cuando se llama cuenta.getCliente().
     */
    @OneToOne(fetch = FetchType.EAGER)
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
