package com.proyecto.servicios.exception;

/**
 * Ni sendTx ni confirmTx permitieron determinar si GestoPago proceso la
 * transaccion (timeout en ambos, o confirmTx tambien fallo). No se sabe si
 * se debe cobrar o no, asi que no se descuenta saldo ni se reintenta solo:
 * la operacion queda registrada para conciliacion manual.
 */
public class TransaccionGestoPagoIndeterminadaException extends RuntimeException {

    public TransaccionGestoPagoIndeterminadaException(String message) {
        super(message);
    }
}
