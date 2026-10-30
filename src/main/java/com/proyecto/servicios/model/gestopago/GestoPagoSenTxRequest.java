package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * Payload de negocio de sendTx.do/confirmTx.do, tal como lo describe la
 * especificacion PuntoRed ("sendTx" request table). Esta clase se serializa
 * a JSON y ese JSON es lo que se cifra con GestoPagoAesCipher antes de
 * mandarlo como el campo "signed" del body.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GestoPagoSenTxRequest {

    private final Integer idProducto;

    private final Integer idServicio;

    private final String telefono;

    private final String horaLocal;

    private final String referencia;

    @JsonProperty("montoPago")
    private final Double montoPago;

    private final String upc;

    private final String unidad;
}
