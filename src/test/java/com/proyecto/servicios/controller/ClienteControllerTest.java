package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.clientes.ClienteActualizaRequest;
import com.proyecto.servicios.model.clientes.ClienteBusquedaRequest;
import com.proyecto.servicios.model.clientes.ClienteRequest;
import com.proyecto.servicios.model.clientes.ClienteResponse;
import com.proyecto.servicios.service.ClienteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteControllerTest {

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private ClienteController clienteController;

    private static ClienteResponse cliente(Integer id) {
        ClienteResponse response = new ClienteResponse();
        response.setId(id);
        response.setNombre("Pedro");
        return response;
    }

    @Test
    void crear_regresa201ConElClienteCreado() {
        ClienteRequest request = new ClienteRequest();
        when(clienteService.crearCliente(request)).thenReturn(cliente(1));

        ResponseEntity<ClienteResponse> respuesta = clienteController.crear(request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody().getId()).isEqualTo(1);
    }

    @Test
    void obtenerTodos_regresa200ConLaLista() {
        when(clienteService.obtenerTodos()).thenReturn(List.of(cliente(1), cliente(2)));

        ResponseEntity<List<ClienteResponse>> respuesta = clienteController.obtenerTodos();

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(2);
    }

    @Test
    void obtenerPorId_regresa200ConElCliente() {
        when(clienteService.obtenerPorId(5)).thenReturn(cliente(5));

        ResponseEntity<ClienteResponse> respuesta = clienteController.obtenerPorId(5);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().getId()).isEqualTo(5);
    }

    @Test
    void actualizar_regresa200ConElClienteActualizado() {
        ClienteActualizaRequest request = new ClienteActualizaRequest();
        when(clienteService.actualizarCliente(5, request)).thenReturn(cliente(5));

        ResponseEntity<ClienteResponse> respuesta = clienteController.actualizar(5, request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().getId()).isEqualTo(5);
    }

    @Test
    void eliminar_regresa204SinCuerpo() {
        ResponseEntity<Void> respuesta = clienteController.eliminar(5);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(respuesta.getBody()).isNull();
        verify(clienteService).eliminarCliente(5);
    }

    @Test
    void buscar_regresa200ConLosClientesQueCoinciden() {
        ClienteBusquedaRequest filtro = new ClienteBusquedaRequest();
        filtro.setActivo(true);
        when(clienteService.buscarClientes(filtro)).thenReturn(List.of(cliente(3)));

        ResponseEntity<List<ClienteResponse>> respuesta = clienteController.buscar(filtro);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).singleElement().satisfies(c -> assertThat(c.getId()).isEqualTo(3));
    }
}
