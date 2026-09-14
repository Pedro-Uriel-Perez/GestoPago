package com.proyecto.servicios.model.productlist;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductListApiResponse {

    private List<ProductoExternoDTO> productos;

    private String message;

    private Integer status;
}
