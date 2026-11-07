package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GestoPagoTokenMapperImplTest {

    private final GestoPagoTokenMapper mapper = new GestoPagoTokenMapperImpl();

    private static GestoPagoAuthResponse respuesta(String token, String tipo, Long expira) {
        GestoPagoAuthResponse response = new GestoPagoAuthResponse();
        response.setToken(token);
        response.setTokenType(tipo);
        response.setExpiresIn(expira);
        return response;
    }

    @Test
    void toEntity_copiaTokenTipoYVigencia() {
        GestoPagoToken entidad = mapper.toEntity(respuesta("abc", "Bearer", 3600L));

        assertThat(entidad.getToken()).isEqualTo("abc");
        assertThat(entidad.getTokenType()).isEqualTo("Bearer");
        assertThat(entidad.getExpiresIn()).isEqualTo(3600L);
        assertThat(entidad.getId()).isNull();
    }

    @Test
    void toEntity_respuestaNull_regresaNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void updateEntity_reemplazaSoloLosCamposQueVienen() {
        GestoPagoToken entidad = mapper.toEntity(respuesta("viejo", "Bearer", 100L));

        mapper.updateEntity(respuesta("nuevo", null, null), entidad);

        assertThat(entidad.getToken()).isEqualTo("nuevo");
        assertThat(entidad.getTokenType()).isEqualTo("Bearer");
        assertThat(entidad.getExpiresIn()).isEqualTo(100L);
    }

    @Test
    void updateEntity_conTodosLosCampos_losReemplaza() {
        GestoPagoToken entidad = mapper.toEntity(respuesta("viejo", "Bearer", 100L));

        mapper.updateEntity(respuesta("nuevo", "JWT", 200L), entidad);

        assertThat(entidad.getToken()).isEqualTo("nuevo");
        assertThat(entidad.getTokenType()).isEqualTo("JWT");
        assertThat(entidad.getExpiresIn()).isEqualTo(200L);
    }

    @Test
    void updateEntity_respuestaNull_noCambiaNada() {
        GestoPagoToken entidad = mapper.toEntity(respuesta("viejo", "Bearer", 100L));

        mapper.updateEntity(null, entidad);

        assertThat(entidad.getToken()).isEqualTo("viejo");
    }
}
