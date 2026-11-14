package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.clientes.CuentaResponse;
import com.proyecto.servicios.model.clientes.DepositoRequest;
import com.proyecto.servicios.model.clientes.SaldoResponse;
import com.proyecto.servicios.service.CuentaService;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping(value = "/cuentas", produces = MediaType.APPLICATION_JSON_VALUE)
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @GetMapping("/{numeroCuenta}")
    public ResponseEntity<CuentaResponse> obtenerPorNumeroCuenta(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.obtenerPorNumeroCuenta(numeroCuenta));
    }

    /**
     * Deposita saldo en la cuenta del cliente autenticado. El clienteId se
     * toma del JWT, no del body, para que nadie abone o consulte cuentas ajenas.
     */
    @PostMapping(value = "/depositos", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CuentaResponse> depositar(@Valid @RequestBody DepositoRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.ok(cuentaService.depositar(request, (Integer) authentication.getPrincipal()));
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
