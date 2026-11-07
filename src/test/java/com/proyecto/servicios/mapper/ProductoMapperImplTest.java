package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.model.productlist.ProductoExternoDTO;
import com.proyecto.servicios.model.productlist.ProductoResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductoMapperImplTest {

    private final ProductoMapper mapper = new ProductoMapperImpl();

    private static ProductoExternoDTO externo() {
        ProductoExternoDTO dto = new ProductoExternoDTO();
        dto.setIdProducto(14302);
        dto.setIdServicio(140);
        dto.setServicio("Telcel");
        dto.setProducto("Recarga Telcel $50");
        dto.setIdCatTipoServicio(1);
        dto.setTipoFront(1);
        dto.setHasDigitoVerificador(false);
        dto.setTipoReferencia("TELEFONO");
        dto.setPrecio("50.00");
        dto.setLegend("Recarga de tiempo aire");
        return dto;
    }

    @Test
    void toEntity_copiaTodosLosDatosDelCatalogo() {
        GestoPagoProducto entidad = mapper.toEntity(externo());

        assertThat(entidad.getIdProducto()).isEqualTo(14302);
        assertThat(entidad.getIdServicio()).isEqualTo(140);
        assertThat(entidad.getServicio()).isEqualTo("Telcel");
        assertThat(entidad.getProducto()).isEqualTo("Recarga Telcel $50");
        assertThat(entidad.getIdCatTipoServicio()).isEqualTo(1);
        assertThat(entidad.getTipoFront()).isEqualTo(1);
        assertThat(entidad.getHasDigitoVerificador()).isFalse();
        assertThat(entidad.getTipoReferencia()).isEqualTo("TELEFONO");
        assertThat(entidad.getPrecio()).isEqualTo("50.00");
        assertThat(entidad.getLegend()).isEqualTo("Recarga de tiempo aire");
        assertThat(entidad.getId()).isNull();
    }

    @Test
    void toEntityList_mapeaCadaProducto() {
        List<GestoPagoProducto> entidades = mapper.toEntityList(List.of(externo(), externo()));

        assertThat(entidades).hasSize(2).allSatisfy(e -> assertThat(e.getIdProducto()).isEqualTo(14302));
    }

    @Test
    void toResponse_renombraProductoServicioYLeyenda() {
        GestoPagoProducto entidad = mapper.toEntity(externo());
        entidad.setId(5);
        entidad.setFechaActualizacion(LocalDateTime.of(2026, 11, 6, 6, 0));

        ProductoResponse response = mapper.toResponse(entidad);

        assertThat(response.getId()).isEqualTo(5);
        assertThat(response.getIdProducto()).isEqualTo(14302);
        assertThat(response.getIdServicio()).isEqualTo(140);
        assertThat(response.getIdCatTipoServicio()).isEqualTo(1);
        assertThat(response.getNombreProducto()).isEqualTo("Recarga Telcel $50");
        assertThat(response.getNombreServicio()).isEqualTo("Telcel");
        assertThat(response.getDescripcion()).isEqualTo("Recarga de tiempo aire");
        assertThat(response.getTipoFront()).isEqualTo(1);
        assertThat(response.getTipoReferencia()).isEqualTo("TELEFONO");
        assertThat(response.getPrecio()).isEqualTo("50.00");
        assertThat(response.getFechaActualizacion()).isEqualTo(LocalDateTime.of(2026, 11, 6, 6, 0));
    }

    @Test
    void toResponseList_mapeaCadaProducto() {
        List<ProductoResponse> responses = mapper.toResponseList(List.of(mapper.toEntity(externo())));

        assertThat(responses).singleElement()
                .satisfies(r -> assertThat(r.getNombreServicio()).isEqualTo("Telcel"));
    }

    @Test
    void metodos_conEntradaNull_regresanNull() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toEntityList(null)).isNull();
        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toResponseList(null)).isNull();
    }
}
