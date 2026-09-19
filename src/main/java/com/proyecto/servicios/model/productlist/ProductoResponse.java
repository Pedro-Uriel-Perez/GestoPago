package com.proyecto.servicios.model.productlist;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO publico expuesto por GET /productos, independiente de la forma en la
 * que GestoPago entrega el catalogo (XML con atributos).
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductoResponse {

    private Integer idProducto;

    private Integer idServicio;

    private String servicio;

    private String nombre;

    private String descripcion;
}
