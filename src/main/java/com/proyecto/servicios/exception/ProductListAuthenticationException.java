package com.proyecto.servicios.exception;

/**
 * El servicio externo rechazo el Bearer Token (401/403).
 */
public class ProductListAuthenticationException extends ProductListException {

    public ProductListAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
