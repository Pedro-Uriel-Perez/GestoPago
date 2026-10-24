package com.proyecto.servicios.exception;

public class ClienteInactivoException extends RuntimeException {

    public ClienteInactivoException(String message) {
        super(message);
    }
}
