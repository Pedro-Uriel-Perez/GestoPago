package com.proyecto.servicios.exception;

/**
 * El servicio externo respondio con un estado HTTP de error distinto
 * a problemas de autenticacion (4xx/5xx).
 */
public class ProductListUnsuccessfulResponseException extends ProductListException {

    public ProductListUnsuccessfulResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
