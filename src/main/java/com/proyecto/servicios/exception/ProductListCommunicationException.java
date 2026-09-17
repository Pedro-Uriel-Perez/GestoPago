package com.proyecto.servicios.exception;

/**
 * Fallo de comunicacion (red, conexion, etc.) al invocar el servicio
 * externo, distinto de un timeout o de una respuesta HTTP de error.
 */
public class ProductListCommunicationException extends ProductListException {

    public ProductListCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
