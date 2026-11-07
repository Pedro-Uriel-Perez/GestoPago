package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.clientes.NacionalidadResponse;
import com.proyecto.servicios.service.NacionalidadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NacionalidadControllerTest {

    @Mock
    private NacionalidadService nacionalidadService;

    @InjectMocks
    private NacionalidadController nacionalidadController;

    @Test
    void obtenerTodas_regresa200ConElCatalogo() {
        NacionalidadResponse mexicana = new NacionalidadResponse();
        mexicana.setId(1);
        mexicana.setNombre("Mexicana");
        when(nacionalidadService.obtenerTodas()).thenReturn(List.of(mexicana));

        ResponseEntity<List<NacionalidadResponse>> respuesta = nacionalidadController.obtenerTodas();

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).singleElement()
                .satisfies(n -> assertThat(n.getNombre()).isEqualTo("Mexicana"));
    }
}
