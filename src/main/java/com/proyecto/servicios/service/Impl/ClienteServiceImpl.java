package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.entity.clientes.Cuenta;
import com.proyecto.servicios.entity.clientes.Domicilio;
import com.proyecto.servicios.entity.clientes.Saldo;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ClienteYaRegistradoException;
import com.proyecto.servicios.exception.CriterioBusquedaInvalidoException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.clientes.ClienteActualizaRequest;
import com.proyecto.servicios.model.clientes.ClienteRequest;
import com.proyecto.servicios.model.clientes.ClienteResponse;
import com.proyecto.servicios.repositorys.clientes.ClienteRepository;
import com.proyecto.servicios.repositorys.clientes.CuentaRepository;
import com.proyecto.servicios.repositorys.clientes.DomicilioRepository;
import com.proyecto.servicios.repositorys.clientes.SaldoRepository;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.service.CuentaService;
import com.proyecto.servicios.service.LoginService;
import com.proyecto.servicios.service.NacionalidadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class ClienteServiceImpl implements ClienteService {

    private static final int LONGITUD_NUMERO_CUENTA = 10;

    private final ClienteRepository clienteRepository;
    private final DomicilioRepository domicilioRepository;
    private final CuentaRepository cuentaRepository;
    private final SaldoRepository saldoRepository;
    private final ClienteMapper clienteMapper;
    private final CuentaService cuentaService;
    private final LoginService loginService;
    private final NacionalidadService nacionalidadService;
    private final SecureRandom generadorAleatorio = new SecureRandom();

    public ClienteServiceImpl(ClienteRepository clienteRepository,
                               DomicilioRepository domicilioRepository,
                               CuentaRepository cuentaRepository,
                               SaldoRepository saldoRepository,
                               ClienteMapper clienteMapper,
                               CuentaService cuentaService,
                               LoginService loginService,
                               NacionalidadService nacionalidadService) {
        this.clienteRepository = clienteRepository;
        this.domicilioRepository = domicilioRepository;
        this.cuentaRepository = cuentaRepository;
        this.saldoRepository = saldoRepository;
        this.loginService = loginService;
        this.clienteMapper = clienteMapper;
        this.cuentaService = cuentaService;
        this.nacionalidadService = nacionalidadService;
    }

    @Override
    @Transactional
    public ClienteResponse crearCliente(ClienteRequest request) {
        log.info("Registrando nuevo cliente. correo={}", request.getCorreoElectronico());

        validarUnicidad(request.getCurp(), request.getRfc(), request.getCorreoElectronico());

        Cliente clienteMapeado = clienteMapper.toEntity(request);
        clienteMapeado.setNacionalidad(nacionalidadService.buscarPorId(request.getNacionalidadId()));
        Cliente cliente = clienteRepository.save(clienteMapeado);

        Domicilio domicilio = clienteMapper.toEntity(request.getDomicilio());
        domicilio.setCliente(cliente);
        domicilioRepository.save(domicilio);

        Cuenta cuenta = new Cuenta();
        cuenta.setCliente(cliente);
        cuenta.setNumeroCuenta(generarNumeroCuentaUnico());
        cuentaRepository.save(cuenta);

        Saldo saldoInicial = new Saldo();
        saldoInicial.setCuenta(cuenta);
        saldoInicial.setMonto(BigDecimal.ZERO);
        saldoInicial.setTipoMovimiento("APERTURA");
        saldoInicial.setDescripcion("Saldo inicial de apertura de cuenta");
        saldoRepository.save(saldoInicial);

        loginService.registrarCredenciales(cliente, request.getPassword());

        log.info("Cliente registrado correctamente. id={}, cuenta={}", cliente.getId(), cuenta.getNumeroCuenta());
        return ensamblarRespuestaCompleta(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> obtenerTodos() {
        return clienteRepository.findAll().stream()
                .map(this::ensamblarRespuestaCompleta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorId(Integer id) {
        return ensamblarRespuestaCompleta(buscarPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorCurp(String curp) {
        Cliente cliente = clienteRepository.findByCurp(curp)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente con la CURP " + curp));
        return ensamblarRespuestaCompleta(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorRfc(String rfc) {
        Cliente cliente = clienteRepository.findByRfc(rfc)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente con el RFC " + rfc));
        return ensamblarRespuestaCompleta(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorCorreo(String correo) {
        Cliente cliente = clienteRepository.findByCorreoElectronico(correo)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente con el correo " + correo));
        return ensamblarRespuestaCompleta(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorNumeroCuenta(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(
                        "No existe una cuenta con el numero " + numeroCuenta));
        return ensamblarRespuestaCompleta(cuenta.getCliente());
    }

    @Override
    @Transactional
    public ClienteResponse actualizarCliente(Integer id, ClienteActualizaRequest request) {
        Cliente cliente = buscarPorId(id);

        clienteRepository.findByCorreoElectronico(request.getCorreoElectronico())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ClienteYaRegistradoException(
                            "Ya existe un cliente registrado con el correo " + request.getCorreoElectronico());
                });

        clienteMapper.actualizarEntity(request, cliente);
        cliente.setNacionalidad(nacionalidadService.buscarPorId(request.getNacionalidadId()));
        clienteRepository.save(cliente);

        Domicilio domicilio = domicilioRepository.findByClienteId(id)
                .orElseThrow(() -> new ClienteNoEncontradoException(
                        "El cliente con id " + id + " no tiene un domicilio registrado"));
        clienteMapper.actualizarEntity(request.getDomicilio(), domicilio);
        domicilioRepository.save(domicilio);

        log.info("Cliente actualizado. id={}", id);
        return ensamblarRespuestaCompleta(cliente);
    }

    @Override
    @Transactional
    public void eliminarCliente(Integer id) {
        Cliente cliente = buscarPorId(id);
        cliente.setActivo(false);
        clienteRepository.save(cliente);
        loginService.cerrarSesion(id);
        log.info("Cliente desactivado (baja logica) y sesion cerrada. id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse buscarPorCriterio(String curp, String rfc, String correo, String numeroCuenta) {
        List<String> provistos = Stream.of(curp, rfc, correo, numeroCuenta)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return Optional.of(provistos)
                .filter(lista -> lista.size() == 1)
                .map(lista -> resolverBusquedaPorCriterio(curp, rfc, correo, numeroCuenta))
                .orElseThrow(() -> new CriterioBusquedaInvalidoException(
                        "Debes proporcionar exactamente un criterio de busqueda: curp, rfc, correo o numeroCuenta"));
    }

    private ClienteResponse resolverBusquedaPorCriterio(String curp, String rfc, String correo, String numeroCuenta) {
        return Optional.ofNullable(curp).map(this::obtenerPorCurp)
                .or(() -> Optional.ofNullable(rfc).map(this::obtenerPorRfc))
                .or(() -> Optional.ofNullable(correo).map(this::obtenerPorCorreo))
                .or(() -> Optional.ofNullable(numeroCuenta).map(this::obtenerPorNumeroCuenta))
                .orElseThrow(() -> new IllegalStateException("buscarPorCriterio ya garantizo un criterio valido"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> obtenerActivos() {
        return clienteRepository.findByActivoTrue().stream()
                .map(this::ensamblarRespuestaCompleta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> obtenerPorRangoFechas(LocalDateTime desde, LocalDateTime hasta) {
        return clienteRepository.findByFechaRegistroBetween(desde, hasta).stream()
                .map(this::ensamblarRespuestaCompleta)
                .collect(Collectors.toList());
    }

    private void validarUnicidad(String curp, String rfc, String correo) {
        clienteRepository.findByCurp(curp)
                .ifPresent(existente -> {
                    throw new CurpDuplicadaException(curp);
                });
        clienteRepository.findByRfc(rfc)
                .ifPresent(existente -> {
                    throw new RfcDuplicadoException(rfc);
                });
        clienteRepository.findByCorreoElectronico(correo)
                .ifPresent(existente -> {
                    throw new ClienteYaRegistradoException(
                            "Ya existe un cliente registrado con el correo " + correo);
                });
    }

    private Cliente buscarPorId(Integer id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente con id " + id));
    }

    private ClienteResponse ensamblarRespuestaCompleta(Cliente cliente) {
        ClienteResponse response = clienteMapper.toResponse(cliente);

        domicilioRepository.findByClienteId(cliente.getId())
                .map(clienteMapper::toResponse)
                .ifPresent(response::setDomicilio);

        cuentaRepository.findByClienteId(cliente.getId())
                .map(cuentaService::construirRespuesta)
                .ifPresent(response::setCuenta);

        return response;
    }

    private String generarNumeroCuentaUnico() {
        String numeroCuenta = generarCandidatoNumeroCuenta();
        while (cuentaRepository.existsByNumeroCuenta(numeroCuenta)) {
            numeroCuenta = generarCandidatoNumeroCuenta();
        }
        return numeroCuenta;
    }

    private String generarCandidatoNumeroCuenta() {
        StringBuilder numero = new StringBuilder(LONGITUD_NUMERO_CUENTA);
        for (int i = 0; i < LONGITUD_NUMERO_CUENTA; i++) {
            numero.append(generadorAleatorio.nextInt(10));
        }
        return numero.toString();
    }
}
