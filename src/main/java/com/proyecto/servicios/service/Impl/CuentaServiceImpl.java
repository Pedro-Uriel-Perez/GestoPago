package com.proyecto.servicios.service.Impl;

import org.springframework.http.HttpStatus;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.entity.clientes.Cuenta;
import com.proyecto.servicios.entity.clientes.Saldo;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.clientes.CuentaResponse;
import com.proyecto.servicios.model.clientes.DepositoRequest;
import com.proyecto.servicios.model.clientes.SaldoResponse;
import com.proyecto.servicios.repositorys.clientes.CuentaRepository;
import com.proyecto.servicios.repositorys.clientes.SaldoRepository;
import com.proyecto.servicios.service.CuentaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

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
                .toList();
    }

    @Override
    public List<SaldoResponse> obtenerHistorialSaldo(String numeroCuenta) {
        Cuenta cuenta = buscarPorNumeroCuenta(numeroCuenta);
        List<Saldo> historial = saldoRepository.findByCuentaIdOrderByIdDesc(cuenta.getId());
        return clienteMapper.toSaldoResponseList(historial);
    }

    @Override
    public CuentaResponse construirRespuesta(Cuenta cuenta) {
        CuentaResponse response = clienteMapper.toResponse(cuenta);
        BigDecimal saldoActual = saldoRepository.findFirstByCuentaIdOrderByIdDesc(cuenta.getId())
                .map(Saldo::getMonto)
                .orElse(BigDecimal.ZERO);
        response.setSaldoActual(saldoActual);
        return response;
    }

    /**
     * @Transactional a proposito (igual que confirmarCompra): el candado
     * pesimista de findWithLockingById evita que un deposito y una compra
     * simultaneos sobre la misma cuenta lean el mismo saldo y uno pise al otro.
     */
    @Override
    @Transactional
    public CuentaResponse depositar(DepositoRequest request, Integer clienteIdAutenticado) {
        Cuenta cuenta = buscarPorNumeroCuenta(request.getNumeroCuenta());
        if (!cuenta.getCliente().getId().equals(clienteIdAutenticado)) {
            throw new NegocioException(HttpStatus.FORBIDDEN, "La cuenta indicada no pertenece al cliente autenticado");
        }
        if (!"ACTIVA".equals(cuenta.getEstatus())) {
            throw new NegocioException(HttpStatus.CONFLICT, "La cuenta no esta activa, no puede recibir depositos");
        }

        Cuenta cuentaBloqueada = cuentaRepository.findWithLockingById(cuenta.getId())
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "La cuenta ya no existe"));
        BigDecimal saldoActual = saldoRepository.findFirstByCuentaIdOrderByIdDesc(cuentaBloqueada.getId())
                .map(Saldo::getMonto)
                .orElse(BigDecimal.ZERO);

        Saldo movimiento = new Saldo();
        movimiento.setCuenta(cuentaBloqueada);
        movimiento.setMonto(saldoActual.add(request.getMonto()));
        movimiento.setTipoMovimiento("DEPOSITO");
        movimiento.setDescripcion("Deposito a la cuenta");
        saldoRepository.save(movimiento);

        log.info("Deposito registrado. cuentaId={}, clienteId={}", cuentaBloqueada.getId(), clienteIdAutenticado);
        return construirRespuesta(cuentaBloqueada);
    }

    private Cuenta buscarPorNumeroCuenta(String numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND,
                        "No existe una cuenta con el numero " + numeroCuenta));
    }
}
