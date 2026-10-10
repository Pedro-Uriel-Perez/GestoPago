package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.clientes.Cuenta;
import com.proyecto.servicios.entity.clientes.Saldo;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.clientes.CuentaResponse;
import com.proyecto.servicios.model.clientes.SaldoResponse;
import com.proyecto.servicios.repositorys.clientes.CuentaRepository;
import com.proyecto.servicios.repositorys.clientes.SaldoRepository;
import com.proyecto.servicios.service.CuentaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CuentaServiceImpl implements CuentaService {

    private final CuentaRepository cuentaRepository;
    private final SaldoRepository saldoRepository;
    private final ClienteMapper clienteMapper;

    public CuentaServiceImpl(CuentaRepository cuentaRepository,
                              SaldoRepository saldoRepository,
                              ClienteMapper clienteMapper) {
        this.cuentaRepository = cuentaRepository;
        this.saldoRepository = saldoRepository;
        this.clienteMapper = clienteMapper;
    }

    @Override
    public CuentaResponse obtenerPorNumeroCuenta(String numeroCuenta) {
        log.info("Consultando cuenta numero={}", numeroCuenta);
        Cuenta cuenta = buscarPorNumeroCuenta(numeroCuenta);
        return construirRespuesta(cuenta);
    }

    @Override
    public List<CuentaResponse> obtenerActivas() {
        return cuentaRepository.findByEstatus("ACTIVA").stream()
                .map(this::construirRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    public List<SaldoResponse> obtenerHistorialSaldo(String numeroCuenta) {
        Cuenta cuenta = buscarPorNumeroCuenta(numeroCuenta);
        List<Saldo> historial = saldoRepository.findByCuentaIdOrderByFechaMovimientoDesc(cuenta.getId());
        return clienteMapper.toSaldoResponseList(historial);
    }

    @Override
    public CuentaResponse construirRespuesta(Cuenta cuenta) {
        CuentaResponse response = clienteMapper.toResponse(cuenta);
        BigDecimal saldoActual = saldoRepository.findFirstByCuentaIdOrderByFechaMovimientoDesc(cuenta.getId())
                .map(Saldo::getMonto)
                .orElse(BigDecimal.ZERO);
        response.setSaldoActual(saldoActual);
        return response;
    }

    private Cuenta buscarPorNumeroCuenta(String numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(
                        "No existe una cuenta con el numero " + numeroCuenta));
    }
}
