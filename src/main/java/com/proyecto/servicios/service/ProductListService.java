package com.proyecto.servicios.service;

import com.proyecto.servicios.model.productlist.ProductoResponse;

import java.util.List;

public interface ProductListService {

    List<ProductoResponse> obtenerListaProductos();
}
