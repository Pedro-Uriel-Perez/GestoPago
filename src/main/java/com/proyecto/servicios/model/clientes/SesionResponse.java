package com.proyecto.servicios.model.clientes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Una sesion activa del cliente. "actual" marca la sesion desde la que se
 * hizo la consulta, para que cada app pueda detectar que hay otra sesion
 * abierta en un dispositivo distinto y avisarle al usuario.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SesionResponse {

    private String dispositivo;

    private LocalDateTime fechaInicio;

    private LocalDateTime fechaUltimoAcceso;

    private boolean actual;
}
