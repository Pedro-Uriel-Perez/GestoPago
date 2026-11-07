package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.service.PersonaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonaControllerTest {

    @Mock
    private PersonaService personaService;

    @InjectMocks
    private PersonaController personaController;

    private static GenericResponse respuesta(String mensaje) {
        GenericResponse response = new GenericResponse();
        response.setCodigo(0);
        response.setMensaje(mensaje);
        return response;
    }

    @Test
    void crearUser_regresa200ConLaPersonaCreada() {
        PersonasRequest request = new PersonasRequest();
        PersonaResponse creada = new PersonaResponse();
        creada.setNombre("Pedro");
        creada.setMensaje("Persona creada");
        when(personaService.creaPersona(request)).thenReturn(creada);

        ResponseEntity<GenericResponse> respuesta = personaController.crearUser(request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().getMensaje()).isEqualTo("Persona creada");
    }

    @Test
    void actualizUser_regresa200ConElResultado() {
        PersonasRequest request = new PersonasRequest();
        when(personaService.actualizaPersona(request)).thenReturn(respuesta("Persona actualizada"));

        ResponseEntity<GenericResponse> respuesta = personaController.actualizUser(request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().getMensaje()).isEqualTo("Persona actualizada");
    }

    @Test
    void eliminarUser_regresa200ConElResultado() {
        EliminaPersonaRequest request = new EliminaPersonaRequest();
        when(personaService.eliminaPersona(request)).thenReturn(respuesta("Persona eliminada"));

        ResponseEntity<GenericResponse> respuesta = personaController.actualizUser(request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().getMensaje()).isEqualTo("Persona eliminada");
    }
}
