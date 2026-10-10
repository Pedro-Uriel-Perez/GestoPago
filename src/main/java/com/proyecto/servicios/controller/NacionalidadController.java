package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.clientes.NacionalidadResponse;
import com.proyecto.servicios.service.NacionalidadService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/nacionalidades", produces = MediaType.APPLICATION_JSON_VALUE)
public class NacionalidadController {

    private final NacionalidadService nacionalidadService;

    public NacionalidadController(NacionalidadService nacionalidadService) {
        this.nacionalidadService = nacionalidadService;
    }

    @GetMapping
    public ResponseEntity<List<NacionalidadResponse>> obtenerTodas() {
        return ResponseEntity.ok(nacionalidadService.obtenerTodas());
    }
}
