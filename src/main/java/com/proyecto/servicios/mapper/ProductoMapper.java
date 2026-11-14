package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.model.productlist.ProductoExternoDTO;
import com.proyecto.servicios.model.productlist.ProductoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    GestoPagoProducto toEntity(ProductoExternoDTO externo);

    List<GestoPagoProducto> toEntityList(List<ProductoExternoDTO> externos);

    @Mapping(target = "nombreProducto", source = "producto")
    @Mapping(target = "nombreServicio", source = "servicio")
    @Mapping(target = "descripcion", source = "legend")
    @Mapping(target = "categoria", ignore = true)
    @Mapping(target = "logoUrl", ignore = true)
    @Mapping(target = "imagenReferenciaUrl", ignore = true)
    @Mapping(target = "permiteValidarReferencia", ignore = true)
    ProductoResponse toResponse(GestoPagoProducto entidad);

    List<ProductoResponse> toResponseList(List<GestoPagoProducto> entidades);
}
