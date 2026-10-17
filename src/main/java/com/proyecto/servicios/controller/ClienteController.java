package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.clientes.ClienteActualizaRequest;
import com.proyecto.servicios.model.clientes.ClienteBusquedaRequest;
import com.proyecto.servicios.model.clientes.ClienteRequest;
import com.proyecto.servicios.model.clientes.ClienteResponse;
import com.proyecto.servicios.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/clientes", produces = MediaType.APPLICATION_JSON_VALUE)
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
        return new ResponseEntity<>(clienteService.crearCliente(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> obtenerTodos() {
        return ResponseEntity.ok(clienteService.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(clienteService.obtenerPorId(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> actualizar(@PathVariable Integer id,
                                                       @Valid @RequestBody ClienteActualizaRequest request) {
        return ResponseEntity.ok(clienteService.actualizarCliente(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        clienteService.eliminarCliente(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reemplaza los antiguos /buscar (query params), /activos y
     * /rango-fechas: un unico endpoint que recibe el filtro en el cuerpo
     * (@RequestBody) y combina con AND solo los criterios que se manden
     * (curp, rfc, correo, numeroCuenta, activo, rango de fecha de
     * registro). Si no coincide ningun cliente devuelve 200 con lista
     * vacia, no 404.
     */
    @PostMapping(value = "/buscar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ClienteResponse>> buscar(@Valid @RequestBody ClienteBusquedaRequest filtro) {
        return ResponseEntity.ok(clienteService.buscarClientes(filtro));
    }
}
