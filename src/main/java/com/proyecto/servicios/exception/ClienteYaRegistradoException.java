package com.proyecto.servicios.exception;

/**
 * Ya existe un cliente que choca con una regla de unicidad (correo, o via
 * las subclases CurpDuplicadaException/RfcDuplicadoException).
 */
public class ClienteYaRegistradoException extends RuntimeException {

    public ClienteYaRegistradoException(String message) {
        super(message);
    }
}
