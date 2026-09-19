package com.proyecto.servicios.mapper;

import com.proyecto.servicios.model.productlist.ProductoExternoDTO;
import com.proyecto.servicios.model.productlist.ProductoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductoMapper {

    @Mapping(target = "nombre", source = "producto")
    @Mapping(target = "descripcion", source = "legend")
    ProductoResponse toResponse(ProductoExternoDTO externo);

    List<ProductoResponse> toResponseList(List<ProductoExternoDTO> externos);
}
