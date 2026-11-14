package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.compras.CompraConfirmarRequest;
import com.proyecto.servicios.model.compras.CompraIniciadaResponse;
import com.proyecto.servicios.model.compras.CompraRequest;
import com.proyecto.servicios.model.compras.CompraResultadoResponse;
import com.proyecto.servicios.model.compras.PagoResponse;
import com.proyecto.servicios.model.compras.ReferenciaVerificadaResponse;
import com.proyecto.servicios.model.compras.VerificarReferenciaRequest;
import com.proyecto.servicios.service.CompraService;
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

/**
 * El clienteId autenticado se toma del principal que deja
 * JwtAuthenticationFilter (no de un campo del body), para que un cliente
 * con sesion valida no pueda operar sobre la cuenta de otro mandando un
 * numeroCuenta ajeno.
 */
@RestController
@RequestMapping(value = "/compras", produces = MediaType.APPLICATION_JSON_VALUE)
public class CompraController {

    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CompraIniciadaResponse> iniciar(@Valid @RequestBody CompraRequest request,
                                                          Authentication authentication) {
        return ResponseEntity.ok(compraService.iniciarCompra(request, clienteIdDe(authentication)));
    }

    @PostMapping(value = "/confirmar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CompraResultadoResponse> confirmar(@Valid @RequestBody CompraConfirmarRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(compraService.confirmarCompra(request, clienteIdDe(authentication)));
    }

    /**
     * Valida la referencia de un pago de servicio con GestoPago antes de
     * iniciar el pago.
     */
    @PostMapping(value = "/verificar-referencia", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReferenciaVerificadaResponse> verificarReferencia(
            @Valid @RequestBody VerificarReferenciaRequest request) {
        return ResponseEntity.ok(compraService.verificarReferencia(request));
    }

    /** Historial de pagos del cliente autenticado. */
    @GetMapping
    public ResponseEntity<List<PagoResponse>> historial(Authentication authentication) {
        return ResponseEntity.ok(compraService.obtenerHistorial(clienteIdDe(authentication)));
    }

    /** Detalle de un pago del cliente autenticado. */
    @GetMapping("/{pagoId}")
    public ResponseEntity<PagoResponse> detalle(@PathVariable Integer pagoId, Authentication authentication) {
        return ResponseEntity.ok(compraService.obtenerPago(pagoId, clienteIdDe(authentication)));
    }

    private Integer clienteIdDe(Authentication authentication) {
        return (Integer) authentication.getPrincipal();
    }
}
