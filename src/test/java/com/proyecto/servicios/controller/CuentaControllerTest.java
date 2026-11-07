package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.clientes.CuentaResponse;
import com.proyecto.servicios.model.clientes.SaldoResponse;
import com.proyecto.servicios.service.CuentaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentaControllerTest {

    private static final String NUMERO_CUENTA = "1234567890";

    @Mock
    private CuentaService cuentaService;

    @InjectMocks
    private CuentaController cuentaController;

    private static CuentaResponse cuenta() {
        CuentaResponse response = new CuentaResponse();
        response.setNumeroCuenta(NUMERO_CUENTA);
        response.setSaldoActual(new BigDecimal("1000.00"));
        return response;
    }

    @Test
    void obtenerPorNumeroCuenta_regresa200ConLaCuenta() {
        when(cuentaService.obtenerPorNumeroCuenta(NUMERO_CUENTA)).thenReturn(cuenta());

        ResponseEntity<CuentaResponse> respuesta = cuentaController.obtenerPorNumeroCuenta(NUMERO_CUENTA);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().getSaldoActual()).isEqualByComparingTo("1000.00");
    }

    @Test
    void obtenerActivas_regresa200ConLasCuentasActivas() {
        when(cuentaService.obtenerActivas()).thenReturn(List.of(cuenta()));

        ResponseEntity<List<CuentaResponse>> respuesta = cuentaController.obtenerActivas();

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
    }

    @Test
    void obtenerHistorialSaldo_regresa200ConLosMovimientos() {
        SaldoResponse movimiento = new SaldoResponse();
        movimiento.setTipoMovimiento("DEPOSITO");
        when(cuentaService.obtenerHistorialSaldo(NUMERO_CUENTA)).thenReturn(List.of(movimiento));

        ResponseEntity<List<SaldoResponse>> respuesta = cuentaController.obtenerHistorialSaldo(NUMERO_CUENTA);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).singleElement()
                .satisfies(s -> assertThat(s.getTipoMovimiento()).isEqualTo("DEPOSITO"));
    }
}
