package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.clientes.CuentaResponse;
import com.proyecto.servicios.model.clientes.SaldoResponse;
import com.proyecto.servicios.service.CuentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/cuentas", produces = MediaType.APPLICATION_JSON_VALUE)
public class CuentaController {

    @Autowired
    private CuentaService cuentaService;

    @GetMapping("/{numeroCuenta}")
    public ResponseEntity<CuentaResponse> obtenerPorNumeroCuenta(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.obtenerPorNumeroCuenta(numeroCuenta));
    }

    @GetMapping("/activas")
    public ResponseEntity<List<CuentaResponse>> obtenerActivas() {
        return ResponseEntity.ok(cuentaService.obtenerActivas());
    }

    @GetMapping("/{numeroCuenta}/saldos")
    public ResponseEntity<List<SaldoResponse>> obtenerHistorialSaldo(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.obtenerHistorialSaldo(numeroCuenta));
    }
}
