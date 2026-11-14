package com.proyecto.servicios.model.compras;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Resultado de validar una referencia con GestoPago (verifyReference.do).
 * estado: VALIDA, INVALIDA o NO_VERIFICABLE (GestoPago no ofrece consulta en
 * linea para ese servicio, solo se puede validar el formato).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReferenciaVerificadaResponse {

    public static final String VALIDA = "VALIDA";
    public static final String INVALIDA = "INVALIDA";
    public static final String NO_VERIFICABLE = "NO_VERIFICABLE";

    private String estado;

    private String mensaje;

    /** Adeudo que GestoPago reporta para la referencia; null si no lo informa. */
    private BigDecimal saldoPendiente;
}
