package com.proyecto.servicios.exception;

/**
 * No existe un token GestoPago activo (vigente) para el
 * idDistribuidor/codigoDispositivo configurados. El token se renueva
 * periodicamente en GestoPagoTokenServiceImpl; si aun no se ha generado
 * ninguno, esta excepcion evita seguir la llamada sin autenticacion.
 */
public class GestoPagoTokenNoDisponibleException extends RuntimeException {

    public GestoPagoTokenNoDisponibleException(String message) {
        super(message);
    }
}
