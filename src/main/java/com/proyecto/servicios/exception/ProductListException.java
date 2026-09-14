package com.proyecto.servicios.exception;

/**
 * Excepcion base para cualquier falla al integrar con el servicio
 * externo de lista de productos.
 */
public class ProductListException extends RuntimeException {

    public ProductListException(String message, Throwable cause) {
        super(message, cause);
    }
}
