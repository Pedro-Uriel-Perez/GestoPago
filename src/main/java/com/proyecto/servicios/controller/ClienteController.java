package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.clientes.ClienteActualizaRequest;
import com.proyecto.servicios.model.clientes.ClienteRequest;
import com.proyecto.servicios.model.clientes.ClienteResponse;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.validation.PatronesValidacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Validated a nivel de clase es lo que hace que las anotaciones de
 * Bean Validation en los @RequestParam de abajo (ej. buscar) realmente se
 * evaluen; sin esto, @Pattern/@Email en un parametro suelto no hacen nada.
 * El fallo se atrapa como ConstraintViolationException en
 * GlobalExceptionHandler, igual que los demas errores de validacion.
 */
@Validated
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
     * Reemplaza los antiguos /curp/{curp}, /rfc/{rfc}, /correo/{correo} y
     * /cuenta/{numeroCuenta} (uno por cada campo, con @PathVariable) por un
     * unico endpoint con @RequestParam: se manda exactamente uno de los
     * cuatro. El service devuelve 400 (CriterioBusquedaInvalidoException) si
     * se manda cero o mas de uno.
     */
    @GetMapping("/buscar")
    public ResponseEntity<ClienteResponse> buscar(
            @RequestParam(required = false) @Pattern(regexp = PatronesValidacion.CURP, message = "La CURP no tiene un formato valido") String curp,
            @RequestParam(required = false) @Pattern(regexp = PatronesValidacion.RFC, message = "El RFC no tiene un formato valido") String rfc,
            @RequestParam(required = false) @Email(message = "El correo electronico no tiene un formato valido") String correo,
            @RequestParam(required = false) @Pattern(regexp = PatronesValidacion.NUMERO_CUENTA, message = "El numero de cuenta debe contener exactamente 10 digitos") String numeroCuenta) {
        return ResponseEntity.ok(clienteService.buscarPorCriterio(curp, rfc, correo, numeroCuenta));
    }

    @GetMapping("/activos")
    public ResponseEntity<List<ClienteResponse>> obtenerActivos() {
        return ResponseEntity.ok(clienteService.obtenerActivos());
    }

    @GetMapping("/rango-fechas")
    public ResponseEntity<List<ClienteResponse>> obtenerPorRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return ResponseEntity.ok(clienteService.obtenerPorRangoFechas(desde, hasta));
    }
}
