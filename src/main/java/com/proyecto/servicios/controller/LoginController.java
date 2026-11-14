package com.proyecto.servicios.controller;

import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.model.clientes.LoginRequest;
import com.proyecto.servicios.model.clientes.LoginResponse;
import com.proyecto.servicios.model.clientes.SesionResponse;
import com.proyecto.servicios.service.LoginService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * El clienteId y la sesion se toman de la autenticacion que deja
 * JwtAuthenticationFilter (principal = clienteId, details = id de sesion),
 * asi cada dispositivo solo puede cerrar o consultar lo suyo.
 */
@RestController
@RequestMapping("/login")
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> iniciarSesion(@Valid @RequestBody LoginRequest request) {
        String jwt = loginService.iniciarSesion(
                request.getCorreoElectronico(), request.getPassword(), request.getDispositivo());
        return ResponseEntity.ok(new LoginResponse(jwt));
    }

    /**
     * Cierra la sesion del dispositivo que hace la peticion; la sesion del
     * otro dispositivo (web o movil) sigue abierta.
     */
    @PostMapping("/{clienteId}/cerrar")
    public ResponseEntity<Void> cerrarSesion(@PathVariable Integer clienteId, Authentication authentication) {
        if (!clienteId.equals(authentication.getPrincipal())) {
            throw new NegocioException(HttpStatus.FORBIDDEN, "No puedes cerrar la sesion de otro cliente");
        }
        loginService.cerrarSesion(clienteId, (String) authentication.getDetails());
        return ResponseEntity.noContent().build();
    }

    /**
     * Sesiones activas del cliente autenticado. Las apps lo consultan para
     * avisar cuando se inicia sesion en otro dispositivo.
     */
    @GetMapping(value = "/sesiones", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SesionResponse>> obtenerSesiones(Authentication authentication) {
        return ResponseEntity.ok(loginService.obtenerSesionesActivas(
                (Integer) authentication.getPrincipal(), (String) authentication.getDetails()));
    }
}
