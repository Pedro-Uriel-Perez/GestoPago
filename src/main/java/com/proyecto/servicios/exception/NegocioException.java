package com.proyecto.servicios.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Unica excepcion de negocio de la aplicacion. En lugar de una clase por
 * cada error (cliente no encontrado, saldo insuficiente, etc.), cada caso
 * se lanza con esta misma clase indicando el status HTTP con el que debe
 * responder la API y el mensaje para el cliente. GlobalExceptionHandler la
 * convierte en la respuesta con un solo manejador.
 */
@Getter
public class NegocioException extends RuntimeException {

    private final HttpStatus status;

    public NegocioException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public NegocioException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
