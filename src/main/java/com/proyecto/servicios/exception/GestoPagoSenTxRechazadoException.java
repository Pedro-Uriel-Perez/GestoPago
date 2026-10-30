package com.proyecto.servicios.exception;

/**
 * GestoPago respondio a sendTx/confirmTx con un CODIGO distinto de "01"
 * (ej. "02" saldo insuficiente del distribuidor, "06" transaccion
 * duplicada). El mensaje ya viene en espanol desde GestoPago (campo TEXTO),
 * asi que se reenvia tal cual al cliente.
 */
public class GestoPagoSenTxRechazadoException extends RuntimeException {

    public GestoPagoSenTxRechazadoException(String message) {
        super(message);
    }
}
