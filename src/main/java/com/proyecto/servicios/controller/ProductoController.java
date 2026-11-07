package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.productlist.ProductoListResponse;
import com.proyecto.servicios.service.ProductListService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductoController {

    private final ProductListService productListService;

    public ProductoController(ProductListService productListService) {
        this.productListService = productListService;
    }

    @GetMapping(value = "/productos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductoListResponse> obtenerProductos() {
        ProductoListResponse respuesta = new ProductoListResponse(
                "Datos consultados correctamente",
                productListService.obtenerListaProductos());
        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }
}
