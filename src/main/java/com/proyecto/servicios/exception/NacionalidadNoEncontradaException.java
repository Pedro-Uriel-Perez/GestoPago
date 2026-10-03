package com.proyecto.servicios.exception;

public class NacionalidadNoEncontradaException extends RuntimeException {

    public NacionalidadNoEncontradaException(Integer nacionalidadId) {
        super("No existe una nacionalidad con id " + nacionalidadId);
    }
}
