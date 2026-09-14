package com.proyecto.servicios.mapper;

import com.proyecto.servicios.model.productlist.ProductoExternoDTO;
import com.proyecto.servicios.model.productlist.ProductoResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductoMapper {

    ProductoResponse toResponse(ProductoExternoDTO externo);

    List<ProductoResponse> toResponseList(List<ProductoExternoDTO> externos);
}
