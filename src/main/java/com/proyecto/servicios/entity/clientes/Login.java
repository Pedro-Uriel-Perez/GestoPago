package com.proyecto.servicios.entity.clientes;

import com.proyecto.servicios.security.AesDoubleArrayConverter;
import com.proyecto.servicios.security.AesStringConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Tabla de seguridad/sesion del cliente (1:1). correoLogin, nombreCifrado,
 * jwtToken y datosBiometricos se cifran con AES (ver paquete security) antes
 * de guardarse. sesionActiva y fechaUltimoAcceso quedan sin cifrar a
 * proposito: la tarea programada que cierra sesiones inactivas necesita
 * compararlas directamente en SQL.
 */
@Entity
@Table(name = "logins")
@Getter
@Setter
public class Login {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    private Cliente cliente;

    @Convert(converter = AesStringConverter.class)
    @Column(name = "correo_login", nullable = false, columnDefinition = "TEXT")
    private String correoLogin;

    @Convert(converter = AesStringConverter.class)
    @Column(name = "nombre_cifrado", nullable = false, columnDefinition = "TEXT")
    private String nombreCifrado;

    @Convert(converter = AesStringConverter.class)
    @Column(name = "jwt_token", columnDefinition = "TEXT")
    private String jwtToken;

    @Convert(converter = AesDoubleArrayConverter.class)
    @Column(name = "datos_biometricos", columnDefinition = "TEXT")
    private double[] datosBiometricos;

    @Column(name = "sesion_activa", nullable = false)
    private Boolean sesionActiva = false;

    @Column(name = "fecha_ultimo_acceso")
    private LocalDateTime fechaUltimoAcceso;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void alCrear() {
        fechaCreacion = LocalDateTime.now();
    }
}
