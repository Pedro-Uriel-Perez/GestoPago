package com.proyecto.servicios.service.Impl;

import org.springframework.http.HttpStatus;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.entity.clientes.Cuenta;
import com.proyecto.servicios.entity.clientes.Saldo;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.clientes.CuentaResponse;
import com.proyecto.servicios.model.clientes.DepositoRequest;
import com.proyecto.servicios.repositorys.clientes.CuentaRepository;
import com.proyecto.servicios.repositorys.clientes.SaldoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

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
        when(saldoRepository.findFirstByCuentaIdOrderByIdDesc(10)).thenReturn(Optional.of(ultimoSaldo));

        CuentaResponse resultado = cuentaService.obtenerPorNumeroCuenta("1234567890");

        assertThat(resultado.getSaldoActual()).isEqualByComparingTo("1500.00");
    }

    @Test
    void obtenerPorNumeroCuenta_sinMovimientosDeSaldo_retornaSaldoCero() {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(11);

        when(cuentaRepository.findByNumeroCuenta("0000000001")).thenReturn(Optional.of(cuenta));
        when(clienteMapper.toResponse(cuenta)).thenReturn(new CuentaResponse());
        when(saldoRepository.findFirstByCuentaIdOrderByIdDesc(11)).thenReturn(Optional.empty());

        CuentaResponse resultado = cuentaService.obtenerPorNumeroCuenta("0000000001");

        assertThat(resultado.getSaldoActual()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void obtenerPorNumeroCuenta_noExiste_lanzaError404() {
        when(cuentaRepository.findByNumeroCuenta("9999999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cuentaService.obtenerPorNumeroCuenta("9999999999"))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void obtenerHistorialSaldo_devuelveLaListaMapeada() {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(20);
        List<Saldo> historial = List.of(new Saldo(), new Saldo());

        when(cuentaRepository.findByNumeroCuenta("5555555555")).thenReturn(Optional.of(cuenta));
        when(saldoRepository.findByCuentaIdOrderByIdDesc(20)).thenReturn(historial);
        when(clienteMapper.toSaldoResponseList(historial)).thenReturn(List.of(
                new com.proyecto.servicios.model.clientes.SaldoResponse(),
                new com.proyecto.servicios.model.clientes.SaldoResponse()));

        List<com.proyecto.servicios.model.clientes.SaldoResponse> resultado =
                cuentaService.obtenerHistorialSaldo("5555555555");

        assertThat(resultado).hasSize(2);
    }

    @Test
    void obtenerActivas_regresaSoloLasCuentasConEstatusActiva() {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(30);
        when(cuentaRepository.findByEstatus("ACTIVA")).thenReturn(List.of(cuenta));
        when(clienteMapper.toResponse(any(Cuenta.class))).thenReturn(new CuentaResponse());
        when(saldoRepository.findFirstByCuentaIdOrderByIdDesc(30)).thenReturn(Optional.empty());

        assertThat(cuentaService.obtenerActivas()).hasSize(1);
    }

    private Cuenta cuentaDelCliente(Integer clienteId, String estatus) {
        Cliente cliente = new Cliente();
        cliente.setId(clienteId);
        Cuenta cuenta = new Cuenta();
        cuenta.setId(30);
        cuenta.setNumeroCuenta("1234567890");
        cuenta.setEstatus(estatus);
        cuenta.setCliente(cliente);
        return cuenta;
    }

    private DepositoRequest deposito(String monto) {
        DepositoRequest request = new DepositoRequest();
        request.setNumeroCuenta("1234567890");
        request.setMonto(new BigDecimal(monto));
        return request;
    }

    @Test
    void depositar_sumaElMontoAlSaldoActualYRegistraMovimientoDeposito() {
        Cuenta cuenta = cuentaDelCliente(7, "ACTIVA");
        Saldo saldoPrevio = new Saldo();
        saldoPrevio.setMonto(new BigDecimal("100.00"));
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.findWithLockingById(30)).thenReturn(Optional.of(cuenta));
        when(saldoRepository.findFirstByCuentaIdOrderByIdDesc(30)).thenReturn(Optional.of(saldoPrevio));
        when(clienteMapper.toResponse(any(Cuenta.class))).thenReturn(new CuentaResponse());

        cuentaService.depositar(deposito("250.50"), 7);

        ArgumentCaptor<Saldo> movimiento = ArgumentCaptor.forClass(Saldo.class);
        verify(saldoRepository).save(movimiento.capture());
        assertThat(movimiento.getValue().getMonto()).isEqualByComparingTo("350.50");
        assertThat(movimiento.getValue().getTipoMovimiento()).isEqualTo("DEPOSITO");
    }

    @Test
    void depositar_enCuentaSinMovimientos_parteDeCero() {
        Cuenta cuenta = cuentaDelCliente(7, "ACTIVA");
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.findWithLockingById(30)).thenReturn(Optional.of(cuenta));
        when(saldoRepository.findFirstByCuentaIdOrderByIdDesc(30)).thenReturn(Optional.empty());
        when(clienteMapper.toResponse(any(Cuenta.class))).thenReturn(new CuentaResponse());

        cuentaService.depositar(deposito("80.00"), 7);

        ArgumentCaptor<Saldo> movimiento = ArgumentCaptor.forClass(Saldo.class);
        verify(saldoRepository).save(movimiento.capture());
        assertThat(movimiento.getValue().getMonto()).isEqualByComparingTo("80.00");
    }

    @Test
    void depositar_enCuentaDeOtroCliente_lanzaError403() {
        when(cuentaRepository.findByNumeroCuenta("1234567890"))
                .thenReturn(Optional.of(cuentaDelCliente(7, "ACTIVA")));
        DepositoRequest request = deposito("100.00");

        assertThatThrownBy(() -> cuentaService.depositar(request, 99))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
        verify(saldoRepository, never()).save(any());
    }

    @Test
    void depositar_enCuentaNoActiva_lanzaError409() {
        when(cuentaRepository.findByNumeroCuenta("1234567890"))
                .thenReturn(Optional.of(cuentaDelCliente(7, "CANCELADA")));
        DepositoRequest request = deposito("100.00");

        assertThatThrownBy(() -> cuentaService.depositar(request, 7))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
    }

    @Test
    void depositar_siLaCuentaDesapareceAlBloquearla_lanzaError404() {
        when(cuentaRepository.findByNumeroCuenta("1234567890"))
                .thenReturn(Optional.of(cuentaDelCliente(7, "ACTIVA")));
        when(cuentaRepository.findWithLockingById(30)).thenReturn(Optional.empty());
        DepositoRequest request = deposito("100.00");

        assertThatThrownBy(() -> cuentaService.depositar(request, 7))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }
}
