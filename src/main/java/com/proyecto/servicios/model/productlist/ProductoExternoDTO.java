package com.proyecto.servicios.model.productlist;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Representa un producto tal como lo entrega el servicio externo
 * GET /sistema/service/getProductList.do. Los nombres de campo son una
 * suposicion razonable; deben ajustarse cuando se confirme el contrato real.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductoExternoDTO {

    private String codigo;

    private String nombre;

    private String descripcion;

    private BigDecimal precio;

    private Integer existencia;
}
