package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.sf.Personas;
import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.repositorys.sf.PersonasRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonasServiceImplTest {

    @Mock
    private PersonasRepository personasRepository;

    @InjectMocks
    private PersonasServiceImpl personasService;

    private PersonasRequest request() {
        PersonasRequest request = new PersonasRequest();
        request.setNombre("Ana");
        request.setApellidoP("Gomez");
        request.setApellidoMaterno("Ruiz");
        return request;
    }

    @Test
    void creaPersona_guardaLosDatosYRegresaExito() {
        PersonaResponse respuesta = personasService.creaPersona(request());

        ArgumentCaptor<Personas> guardada = ArgumentCaptor.forClass(Personas.class);
        verify(personasRepository).save(guardada.capture());
        assertThat(guardada.getValue().getNombre()).isEqualTo("Ana");
        assertThat(guardada.getValue().getApellidoP()).isEqualTo("Gomez");
        assertThat(respuesta.getCodigo()).isEqualTo(1);
        assertThat(respuesta.getNombre()).isEqualTo("Ana");
    }

    @Test
    void eliminaPersona_existente_laBorra() {
        Personas persona = new Personas();
        when(personasRepository.findByNombre("Ana")).thenReturn(Optional.of(persona));
        EliminaPersonaRequest request = new EliminaPersonaRequest();
        request.setNombre("Ana");

        GenericResponse respuesta = personasService.eliminaPersona(request);

        verify(personasRepository).delete(persona);
        assertThat(respuesta.getCodigo()).isZero();
    }

    @Test
    void eliminaPersona_noExiste_regresaCodigo1SinBorrar() {
        when(personasRepository.findByNombre("Nadie")).thenReturn(Optional.empty());
        EliminaPersonaRequest request = new EliminaPersonaRequest();
        request.setNombre("Nadie");

        GenericResponse respuesta = personasService.eliminaPersona(request);

        verify(personasRepository, never()).delete(any(Personas.class));
        assertThat(respuesta.getCodigo()).isEqualTo(1);
    }

    @Test
    void actualizaPersona_existente_actualizaLosApellidos() {
        Personas persona = new Personas();
        persona.setNombre("Ana");
        when(personasRepository.findByNombre("Ana")).thenReturn(Optional.of(persona));

        GenericResponse respuesta = personasService.actualizaPersona(request());

        verify(personasRepository).save(persona);
        assertThat(persona.getApellidoP()).isEqualTo("Gomez");
        assertThat(persona.getApellidoMaterno()).isEqualTo("Ruiz");
        assertThat(respuesta.getCodigo()).isZero();
    }

    @Test
    void actualizaPersona_noExiste_regresaCodigo1SinGuardar() {
        when(personasRepository.findByNombre("Ana")).thenReturn(Optional.empty());

        GenericResponse respuesta = personasService.actualizaPersona(request());

        verify(personasRepository, never()).save(any());
        assertThat(respuesta.getCodigo()).isEqualTo(1);
    }
}
