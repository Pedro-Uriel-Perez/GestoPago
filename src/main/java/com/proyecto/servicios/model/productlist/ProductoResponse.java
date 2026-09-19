package com.proyecto.servicios.model.productlist;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO publico expuesto por GET /productos, con los campos confirmados en la
 * tabla "Detailed Response XML Data" de la especificacion real de PuntoRed
 * para GET /sistema/service/getProductList.do.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductoResponse {

    private Integer id;

    private Integer idProducto;

    private Integer idServicio;

    private Integer idCatTipoServicio;

    private String nombreProducto;

    private String nombreServicio;

    private Integer tipoFront;

    private String tipoReferencia;

    private String precio;

    private String descripcion;

    private LocalDateTime fechaActualizacion;
}
