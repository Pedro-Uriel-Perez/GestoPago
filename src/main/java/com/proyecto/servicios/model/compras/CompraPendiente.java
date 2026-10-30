package com.proyecto.servicios.model.compras;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Snapshot de una compra iniciada (POST /compras) mientras se espera la
 * confirmacion del PIN de 2FA (POST /compras/confirmar). Se guarda en Redis
 * con TTL corto, nunca en Postgres: es un estado transitorio, no un
 * historial. El payload que se le manda a GestoPago en la confirmacion debe
 * ser EXACTAMENTE el mismo que se congelo aqui al iniciar la compra.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompraPendiente {

    private Integer clienteId;

    private Integer cuentaId;

    private Integer idProducto;

    private Integer idServicio;

    private String telefono;

    private String referencia;

    private Double montoPago;

    private String upc;

    private String unidad;

    private String horaLocal;

    private BigDecimal monto;

    private String codigoVerificacion;
}
