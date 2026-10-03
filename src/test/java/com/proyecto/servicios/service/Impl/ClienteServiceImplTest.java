package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.entity.clientes.Cuenta;
import com.proyecto.servicios.entity.clientes.Domicilio;
import com.proyecto.servicios.entity.clientes.Nacionalidad;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ClienteYaRegistradoException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.clientes.ClienteRequest;
import com.proyecto.servicios.model.clientes.ClienteResponse;
import com.proyecto.servicios.model.clientes.DomicilioRequest;
import com.proyecto.servicios.repositorys.clientes.ClienteRepository;
import com.proyecto.servicios.repositorys.clientes.CuentaRepository;
import com.proyecto.servicios.repositorys.clientes.DomicilioRepository;
import com.proyecto.servicios.repositorys.clientes.SaldoRepository;
import com.proyecto.servicios.service.CuentaService;
import com.proyecto.servicios.service.LoginService;
import com.proyecto.servicios.service.NacionalidadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private DomicilioRepository domicilioRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private SaldoRepository saldoRepository;

    @Mock
    private ClienteMapper clienteMapper;

    @Mock
    private CuentaService cuentaService;

    @Mock
    private LoginService loginService;

    @Mock
    private NacionalidadService nacionalidadService;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    @Test
    void crearCliente_conDatosUnicos_creaClienteDomicilioCuentaYSaldoInicial() {
        ClienteRequest request = new ClienteRequest();
        request.setCurp("CURP123456789012345".substring(0, 18));
        request.setRfc("RFC123456ABC");
        request.setCorreoElectronico("nuevo@correo.com");
        request.setPassword("contrasena123");
        request.setNacionalidadId(1);
        request.setDomicilio(new DomicilioRequest());

        Cliente clienteMapeado = new Cliente();
        Cliente clienteGuardado = new Cliente();
        clienteGuardado.setId(1);

        Domicilio domicilioMapeado = new Domicilio();

        when(clienteRepository.findByCurp(request.getCurp())).thenReturn(Optional.empty());
        when(clienteRepository.findByRfc(request.getRfc())).thenReturn(Optional.empty());
        when(clienteRepository.findByCorreoElectronico(request.getCorreoElectronico())).thenReturn(Optional.empty());
        when(clienteMapper.toEntity(request)).thenReturn(clienteMapeado);
        when(nacionalidadService.buscarPorId(1)).thenReturn(new Nacionalidad());
        when(clienteRepository.save(clienteMapeado)).thenReturn(clienteGuardado);
        when(clienteMapper.toEntity(request.getDomicilio())).thenReturn(domicilioMapeado);
        when(cuentaRepository.existsByNumeroCuenta(any())).thenReturn(false);
        when(clienteMapper.toResponse(clienteGuardado)).thenReturn(new ClienteResponse());
        when(domicilioRepository.findByClienteId(clienteGuardado.getId())).thenReturn(Optional.empty());
        when(cuentaRepository.findByClienteId(clienteGuardado.getId())).thenReturn(Optional.empty());

        ClienteResponse resultado = clienteService.crearCliente(request);

        assertThat(resultado).isNotNull();
        verify(domicilioRepository).save(domicilioMapeado);
        verify(cuentaRepository).save(any(Cuenta.class));

        ArgumentCaptor<com.proyecto.servicios.entity.clientes.Saldo> saldoCaptor =
                ArgumentCaptor.forClass(com.proyecto.servicios.entity.clientes.Saldo.class);
        verify(saldoRepository).save(saldoCaptor.capture());
        assertThat(saldoCaptor.getValue().getMonto()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saldoCaptor.getValue().getTipoMovimiento()).isEqualTo("APERTURA");

        verify(loginService).registrarCredenciales(clienteGuardado, "contrasena123");
    }

    @Test
    void crearCliente_conCurpYaRegistrada_lanzaCurpDuplicadaException() {
        ClienteRequest request = new ClienteRequest();
        request.setCurp("CURP-EXISTENTE");
        request.setRfc("RFC-NUEVO");
        request.setCorreoElectronico("correo@nuevo.com");

        when(clienteRepository.findByCurp(request.getCurp())).thenReturn(Optional.of(new Cliente()));

        assertThatThrownBy(() -> clienteService.crearCliente(request))
                .isInstanceOf(CurpDuplicadaException.class);

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void crearCliente_conRfcYaRegistrado_lanzaRfcDuplicadoException() {
        ClienteRequest request = new ClienteRequest();
        request.setCurp("CURP-NUEVA");
        request.setRfc("RFC-EXISTENTE");
        request.setCorreoElectronico("correo@nuevo.com");

        when(clienteRepository.findByCurp(request.getCurp())).thenReturn(Optional.empty());
        when(clienteRepository.findByRfc(request.getRfc())).thenReturn(Optional.of(new Cliente()));

        assertThatThrownBy(() -> clienteService.crearCliente(request))
                .isInstanceOf(RfcDuplicadoException.class);

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void crearCliente_conCorreoYaRegistrado_lanzaClienteYaRegistradoException() {
        ClienteRequest request = new ClienteRequest();
        request.setCurp("CURP-NUEVA");
        request.setRfc("RFC-NUEVO");
        request.setCorreoElectronico("correo@existente.com");

        when(clienteRepository.findByCurp(request.getCurp())).thenReturn(Optional.empty());
        when(clienteRepository.findByRfc(request.getRfc())).thenReturn(Optional.empty());
        when(clienteRepository.findByCorreoElectronico(request.getCorreoElectronico()))
                .thenReturn(Optional.of(new Cliente()));

        assertThatThrownBy(() -> clienteService.crearCliente(request))
                .isInstanceOf(ClienteYaRegistradoException.class);

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void obtenerPorId_clienteNoExiste_lanzaClienteNoEncontradoException() {
        when(clienteRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.obtenerPorId(99))
                .isInstanceOf(ClienteNoEncontradoException.class);
    }

    @Test
    void eliminarCliente_existente_loDesactivaSinBorrarloFisicamente() {
        Cliente cliente = new Cliente();
        cliente.setId(5);
        cliente.setActivo(true);
        when(clienteRepository.findById(5)).thenReturn(Optional.of(cliente));

        clienteService.eliminarCliente(5);

        assertThat(cliente.getActivo()).isFalse();
        verify(clienteRepository).save(cliente);
        verify(clienteRepository, never()).delete(any());
        verify(clienteRepository, never()).deleteById(any());
        verify(loginService).cerrarSesion(5);
    }

    @Test
    void obtenerPorNumeroCuenta_cuentaNoExiste_lanzaCuentaNoEncontradaException() {
        when(cuentaRepository.findByNumeroCuenta("0000000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.obtenerPorNumeroCuenta("0000000000"))
                .isInstanceOf(com.proyecto.servicios.exception.CuentaNoEncontradaException.class);
    }
}
