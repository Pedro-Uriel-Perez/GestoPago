package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.productlist.ProductoResponse;
import com.proyecto.servicios.service.ProductListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProductoController {

    @Autowired
    private ProductListService productListService;

    @GetMapping(value = "/productos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ProductoResponse>> obtenerProductos() {
        return new ResponseEntity<>(productListService.obtenerListaProductos(), HttpStatus.OK);
    }
}
