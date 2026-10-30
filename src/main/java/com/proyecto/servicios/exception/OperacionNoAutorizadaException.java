package com.proyecto.servicios.exception;

public class OperacionNoAutorizadaException extends RuntimeException {

    public OperacionNoAutorizadaException(String message) {
        super(message);
    }
}
