package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.clientes.Cuenta;
import com.proyecto.servicios.entity.clientes.Saldo;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.clientes.CuentaResponse;
import com.proyecto.servicios.repositorys.clientes.CuentaRepository;
import com.proyecto.servicios.repositorys.clientes.SaldoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentaServiceImplTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private SaldoRepository saldoRepository;

    @Mock
    private ClienteMapper clienteMapper;

    @InjectMocks
    private CuentaServiceImpl cuentaService;

    @Test
    void obtenerPorNumeroCuenta_existente_incluyeElSaldoMasReciente() {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(10);
        cuenta.setNumeroCuenta("1234567890");

        Saldo ultimoSaldo = new Saldo();
        ultimoSaldo.setMonto(new BigDecimal("1500.00"));

        CuentaResponse respuestaBase = new CuentaResponse();

        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta));
        when(clienteMapper.toResponse(cuenta)).thenReturn(respuestaBase);
        when(saldoRepository.findFirstByCuentaIdOrderByFechaMovimientoDesc(10)).thenReturn(Optional.of(ultimoSaldo));

        CuentaResponse resultado = cuentaService.obtenerPorNumeroCuenta("1234567890");

        assertThat(resultado.getSaldoActual()).isEqualByComparingTo("1500.00");
    }

    @Test
    void obtenerPorNumeroCuenta_sinMovimientosDeSaldo_retornaSaldoCero() {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(11);

        when(cuentaRepository.findByNumeroCuenta("0000000001")).thenReturn(Optional.of(cuenta));
        when(clienteMapper.toResponse(cuenta)).thenReturn(new CuentaResponse());
        when(saldoRepository.findFirstByCuentaIdOrderByFechaMovimientoDesc(11)).thenReturn(Optional.empty());

        CuentaResponse resultado = cuentaService.obtenerPorNumeroCuenta("0000000001");

        assertThat(resultado.getSaldoActual()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void obtenerPorNumeroCuenta_noExiste_lanzaCuentaNoEncontradaException() {
        when(cuentaRepository.findByNumeroCuenta("9999999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cuentaService.obtenerPorNumeroCuenta("9999999999"))
                .isInstanceOf(CuentaNoEncontradaException.class);
    }

    @Test
    void obtenerHistorialSaldo_devuelveLaListaMapeada() {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(20);
        List<Saldo> historial = List.of(new Saldo(), new Saldo());

        when(cuentaRepository.findByNumeroCuenta("5555555555")).thenReturn(Optional.of(cuenta));
        when(saldoRepository.findByCuentaIdOrderByFechaMovimientoDesc(20)).thenReturn(historial);
        when(clienteMapper.toSaldoResponseList(historial)).thenReturn(List.of(
                new com.proyecto.servicios.model.clientes.SaldoResponse(),
                new com.proyecto.servicios.model.clientes.SaldoResponse()));

        List<com.proyecto.servicios.model.clientes.SaldoResponse> resultado =
                cuentaService.obtenerHistorialSaldo("5555555555");

        assertThat(resultado).hasSize(2);
    }
}
