package com.proyecto.servicios.exception;

/**
 * El servicio externo no respondio dentro del tiempo configurado.
 */
public class ProductListTimeoutException extends ProductListException {

    public ProductListTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
