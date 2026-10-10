package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.clientes.LoginRequest;
import com.proyecto.servicios.model.clientes.LoginResponse;
import com.proyecto.servicios.service.LoginService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> iniciarSesion(@Valid @RequestBody LoginRequest request) {
        String jwt = loginService.iniciarSesion(request.getCorreoElectronico(), request.getPassword());
        return ResponseEntity.ok(new LoginResponse(jwt));
    }

    @PostMapping("/{clienteId}/cerrar")
    public ResponseEntity<Void> cerrarSesion(@PathVariable Integer clienteId) {
        loginService.cerrarSesion(clienteId);
        return ResponseEntity.noContent().build();
    }
}
