package com.proyecto.servicios.model.compras;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Un pago del historial del cliente (GET /compras y GET /compras/{id}). */
@Getter
@Setter
@NoArgsConstructor
public class PagoResponse {

    private Integer id;

    private String operacionId;

    private Integer idProducto;

    private Integer idServicio;

    private String nombreProducto;

    private String nombreServicio;

    private String logoUrl;

    private String telefono;

    private String referencia;

    private BigDecimal monto;

    /** EXITOSO, RECHAZADO o PENDIENTE. */
    private String estado;

    /** Folio de la transaccion en GestoPago (ID_TX). */
    private String idTransaccion;

    private String numeroAutorizacion;

    private String mensaje;

    private LocalDateTime fecha;
}
