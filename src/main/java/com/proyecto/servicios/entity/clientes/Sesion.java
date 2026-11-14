package com.proyecto.servicios.entity.clientes;

import com.proyecto.servicios.config.ZonaHoraria;
import com.proyecto.servicios.security.AesStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Sesion de un cliente en UN dispositivo (WEB o MOVIL). Un cliente puede
 * tener una sesion activa por dispositivo al mismo tiempo. identificador es
 * el "sid" que viaja dentro del JWT: permite ubicar la sesion sin tener que
 * buscar por el token, que se guarda cifrado con AES.
 */
@Entity
@Table(name = "sesiones")
@Getter
@Setter
public class Sesion {

    public static final String DISPOSITIVO_WEB = "WEB";
    public static final String DISPOSITIVO_MOVIL = "MOVIL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cliente_id", nullable = false)
    private Integer clienteId;

    @Column(name = "identificador", nullable = false, unique = true, length = 36)
    private String identificador;

    @Column(name = "dispositivo", nullable = false, length = 10)
    private String dispositivo;

    @Convert(converter = AesStringConverter.class)
    @Column(name = "jwt_token", columnDefinition = "TEXT")
    private String jwtToken;

    @Column(name = "activa", nullable = false)
    private Boolean activa = true;

    @Column(name = "fecha_inicio", nullable = false, updatable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_ultimo_acceso", nullable = false)
    private LocalDateTime fechaUltimoAcceso;

    @PrePersist
    void alCrear() {
        fechaInicio = LocalDateTime.now(ZonaHoraria.MEXICO);
        fechaUltimoAcceso = fechaInicio;
    }

    public void cerrar() {
        activa = false;
        jwtToken = null;
    }
}
