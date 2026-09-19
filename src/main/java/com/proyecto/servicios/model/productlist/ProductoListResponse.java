package com.proyecto.servicios.model.productlist;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Envoltura de respuesta de GET /productos: {"mensaje": "...", "data": [...]}.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductoListResponse {

    private String mensaje;
    private List<ProductoResponse> data;

    public ProductoListResponse(String mensaje, List<ProductoResponse> data) {
        this.mensaje = mensaje;
        this.data = data;
    }
}
