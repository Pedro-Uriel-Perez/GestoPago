package com.proyecto.servicios.model.productlist;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO publico expuesto por nuestra API, independiente de la forma
 * en la que el servicio externo entrega la informacion.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductoResponse {

    private String codigo;

    private String nombre;

    private String descripcion;

    private BigDecimal precio;

    private Integer existencia;
}
