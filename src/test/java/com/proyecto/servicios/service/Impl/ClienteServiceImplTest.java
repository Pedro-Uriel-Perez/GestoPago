package com.proyecto.servicios.service.Impl;

import org.springframework.http.HttpStatus;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.entity.clientes.Cuenta;
import com.proyecto.servicios.entity.clientes.Domicilio;
import com.proyecto.servicios.entity.clientes.Nacionalidad;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.clientes.ClienteBusquedaRequest;
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
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
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
    void crearCliente_conCurpYaRegistrada_lanzaError409() {
        ClienteRequest request = new ClienteRequest();
        request.setCurp("CURP-EXISTENTE");
        request.setRfc("RFC-NUEVO");
        request.setCorreoElectronico("correo@nuevo.com");

        when(clienteRepository.findByCurp(request.getCurp())).thenReturn(Optional.of(new Cliente()));

        assertThatThrownBy(() -> clienteService.crearCliente(request))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void crearCliente_conRfcYaRegistrado_lanzaError409() {
        ClienteRequest request = new ClienteRequest();
        request.setCurp("CURP-NUEVA");
        request.setRfc("RFC-EXISTENTE");
        request.setCorreoElectronico("correo@nuevo.com");

        when(clienteRepository.findByCurp(request.getCurp())).thenReturn(Optional.empty());
        when(clienteRepository.findByRfc(request.getRfc())).thenReturn(Optional.of(new Cliente()));

        assertThatThrownBy(() -> clienteService.crearCliente(request))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void crearCliente_conCorreoYaRegistrado_lanzaError409() {
        ClienteRequest request = new ClienteRequest();
        request.setCurp("CURP-NUEVA");
        request.setRfc("RFC-NUEVO");
        request.setCorreoElectronico("correo@existente.com");

        when(clienteRepository.findByCurp(request.getCurp())).thenReturn(Optional.empty());
        when(clienteRepository.findByRfc(request.getRfc())).thenReturn(Optional.empty());
        when(clienteRepository.findByCorreoElectronico(request.getCorreoElectronico()))
                .thenReturn(Optional.of(new Cliente()));

        assertThatThrownBy(() -> clienteService.crearCliente(request))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void obtenerPorId_clienteNoExiste_lanzaError404() {
        when(clienteRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.obtenerPorId(99))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
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
        verify(clienteRepository, never()).delete(any(Cliente.class));
        verify(clienteRepository, never()).deleteById(any());
        verify(loginService).cerrarTodasLasSesiones(5);
    }

    @Test
    void obtenerPorNumeroCuenta_cuentaNoExiste_lanzaError404() {
        when(cuentaRepository.findByNumeroCuenta("0000000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.obtenerPorNumeroCuenta("0000000000"))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void buscarClientes_conFiltroPorCurp_devuelveLosClientesQueCoinciden() {
        Cliente cliente = new Cliente();
        cliente.setId(7);
        when(clienteRepository.findAll(any(Specification.class))).thenReturn(List.of(cliente));
        when(clienteMapper.toResponse(cliente)).thenReturn(new ClienteResponse());
        when(domicilioRepository.findByClienteId(7)).thenReturn(Optional.empty());
        when(cuentaRepository.findByClienteId(7)).thenReturn(Optional.empty());

        ClienteBusquedaRequest filtro = new ClienteBusquedaRequest();
        filtro.setCurp("CURP-X");

        List<ClienteResponse> resultado = clienteService.buscarClientes(filtro);

        assertThat(resultado).hasSize(1);
        verify(clienteRepository).findAll(any(Specification.class));
        verify(cuentaRepository, never()).findByNumeroCuenta(any());
    }

    @Test
    void buscarClientes_sinCoincidencias_devuelveListaVaciaSinLanzarExcepcion() {
        when(clienteRepository.findAll(any(Specification.class))).thenReturn(List.of());

        List<ClienteResponse> resultado = clienteService.buscarClientes(new ClienteBusquedaRequest());

        assertThat(resultado).isEmpty();
    }

    @Test
    void buscarClientes_conNumeroCuentaInexistente_devuelveListaVaciaSinConsultarClientes() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.empty());

        ClienteBusquedaRequest filtro = new ClienteBusquedaRequest();
        filtro.setNumeroCuenta("1234567890");

        List<ClienteResponse> resultado = clienteService.buscarClientes(filtro);

        assertThat(resultado).isEmpty();
        verify(clienteRepository, never()).findAll(any(Specification.class));
    }

    @Test
    void buscarClientes_conNumeroCuentaExistente_filtraPorElClienteDeEsaCuenta() {
        Cliente clienteDeLaCuenta = new Cliente();
        clienteDeLaCuenta.setId(3);
        Cliente otroCliente = new Cliente();
        otroCliente.setId(9);

        Cuenta cuenta = new Cuenta();
        cuenta.setCliente(clienteDeLaCuenta);
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta));
        when(clienteRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(clienteDeLaCuenta, otroCliente));
        when(clienteMapper.toResponse(clienteDeLaCuenta)).thenReturn(new ClienteResponse());
        when(domicilioRepository.findByClienteId(3)).thenReturn(Optional.empty());
        when(cuentaRepository.findByClienteId(3)).thenReturn(Optional.empty());

        ClienteBusquedaRequest filtro = new ClienteBusquedaRequest();
        filtro.setNumeroCuenta("1234567890");

        List<ClienteResponse> resultado = clienteService.buscarClientes(filtro);

        assertThat(resultado).hasSize(1);
        verify(clienteMapper, never()).toResponse(otroCliente);
    }

    // ---------- consultas ----------

    private Cliente clienteConId(Integer id) {
        Cliente cliente = new Cliente();
        cliente.setId(id);
        return cliente;
    }

    private void respuestaArmadaPara(Cliente cliente) {
        when(clienteMapper.toResponse(cliente)).thenReturn(new ClienteResponse());
        when(domicilioRepository.findByClienteId(cliente.getId())).thenReturn(Optional.empty());
        when(cuentaRepository.findByClienteId(cliente.getId())).thenReturn(Optional.empty());
    }

    @Test
    void obtenerTodos_regresaUnaRespuestaPorCadaCliente() {
        Cliente uno = clienteConId(1);
        Cliente dos = clienteConId(2);
        when(clienteRepository.findAll()).thenReturn(List.of(uno, dos));
        respuestaArmadaPara(uno);
        respuestaArmadaPara(dos);

        assertThat(clienteService.obtenerTodos()).hasSize(2);
    }

    @Test
    void obtenerPorId_existente_armaLaRespuestaConDomicilioYCuenta() {
        Cliente cliente = clienteConId(3);
        when(clienteRepository.findById(3)).thenReturn(Optional.of(cliente));
        when(clienteMapper.toResponse(cliente)).thenReturn(new ClienteResponse());
        Domicilio domicilio = new Domicilio();
        when(domicilioRepository.findByClienteId(3)).thenReturn(Optional.of(domicilio));
        when(clienteMapper.toResponse(domicilio)).thenReturn(new com.proyecto.servicios.model.clientes.DomicilioResponse());
        Cuenta cuenta = new Cuenta();
        when(cuentaRepository.findByClienteId(3)).thenReturn(Optional.of(cuenta));
        when(cuentaService.construirRespuesta(cuenta)).thenReturn(new com.proyecto.servicios.model.clientes.CuentaResponse());

        ClienteResponse resultado = clienteService.obtenerPorId(3);

        assertThat(resultado.getDomicilio()).isNotNull();
        assertThat(resultado.getCuenta()).isNotNull();
    }

    @Test
    void obtenerPorCurp_existente_regresaElCliente() {
        Cliente cliente = clienteConId(4);
        when(clienteRepository.findByCurp("CURP")).thenReturn(Optional.of(cliente));
        respuestaArmadaPara(cliente);

        assertThat(clienteService.obtenerPorCurp("CURP")).isNotNull();
    }

    @Test
    void obtenerPorCurp_noExiste_lanzaError404() {
        when(clienteRepository.findByCurp("CURP")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.obtenerPorCurp("CURP"))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void obtenerPorRfc_existente_regresaElCliente() {
        Cliente cliente = clienteConId(5);
        when(clienteRepository.findByRfc("RFC")).thenReturn(Optional.of(cliente));
        respuestaArmadaPara(cliente);

        assertThat(clienteService.obtenerPorRfc("RFC")).isNotNull();
    }

    @Test
    void obtenerPorRfc_noExiste_lanzaError404() {
        when(clienteRepository.findByRfc("RFC")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.obtenerPorRfc("RFC"))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void obtenerPorCorreo_existente_regresaElCliente() {
        Cliente cliente = clienteConId(6);
        when(clienteRepository.findByCorreoElectronico("a@correo.com")).thenReturn(Optional.of(cliente));
        respuestaArmadaPara(cliente);

        assertThat(clienteService.obtenerPorCorreo("a@correo.com")).isNotNull();
    }

    @Test
    void obtenerPorCorreo_noExiste_lanzaError404() {
        when(clienteRepository.findByCorreoElectronico("a@correo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.obtenerPorCorreo("a@correo.com"))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void obtenerPorNumeroCuenta_existente_regresaElClienteDeEsaCuenta() {
        Cliente cliente = clienteConId(7);
        Cuenta cuenta = new Cuenta();
        cuenta.setCliente(cliente);
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta));
        respuestaArmadaPara(cliente);

        assertThat(clienteService.obtenerPorNumeroCuenta("1234567890")).isNotNull();
    }

    // ---------- actualizacion ----------

    private com.proyecto.servicios.model.clientes.ClienteActualizaRequest actualizacion() {
        com.proyecto.servicios.model.clientes.ClienteActualizaRequest request =
                new com.proyecto.servicios.model.clientes.ClienteActualizaRequest();
        request.setCorreoElectronico("nuevo@correo.com");
        request.setNacionalidadId(1);
        request.setDomicilio(new DomicilioRequest());
        return request;
    }

    @Test
    void actualizarCliente_datosValidos_actualizaClienteYDomicilio() {
        Cliente cliente = clienteConId(8);
        Domicilio domicilio = new Domicilio();
        com.proyecto.servicios.model.clientes.ClienteActualizaRequest request = actualizacion();
        when(clienteRepository.findById(8)).thenReturn(Optional.of(cliente));
        when(clienteRepository.findByCorreoElectronico("nuevo@correo.com")).thenReturn(Optional.empty());
        when(nacionalidadService.buscarPorId(1)).thenReturn(new Nacionalidad());
        when(domicilioRepository.findByClienteId(8)).thenReturn(Optional.of(domicilio));
        when(clienteMapper.toResponse(cliente)).thenReturn(new ClienteResponse());
        when(cuentaRepository.findByClienteId(8)).thenReturn(Optional.empty());

        ClienteResponse resultado = clienteService.actualizarCliente(8, request);

        assertThat(resultado).isNotNull();
        verify(clienteMapper).actualizarEntity(request, cliente);
        verify(clienteRepository).save(cliente);
        verify(domicilioRepository).save(domicilio);
    }

    @Test
    void actualizarCliente_conCorreoDeOtroCliente_lanzaError409() {
        Cliente cliente = clienteConId(8);
        when(clienteRepository.findById(8)).thenReturn(Optional.of(cliente));
        when(clienteRepository.findByCorreoElectronico("nuevo@correo.com")).thenReturn(Optional.of(clienteConId(99)));

        var cambios = actualizacion();
        assertThatThrownBy(() -> clienteService.actualizarCliente(8, cambios))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void actualizarCliente_conSuMismoCorreo_noLoConsideraDuplicado() {
        Cliente cliente = clienteConId(8);
        when(clienteRepository.findById(8)).thenReturn(Optional.of(cliente));
        when(clienteRepository.findByCorreoElectronico("nuevo@correo.com")).thenReturn(Optional.of(cliente));
        when(nacionalidadService.buscarPorId(1)).thenReturn(new Nacionalidad());
        when(domicilioRepository.findByClienteId(8)).thenReturn(Optional.of(new Domicilio()));
        when(clienteMapper.toResponse(cliente)).thenReturn(new ClienteResponse());
        when(cuentaRepository.findByClienteId(8)).thenReturn(Optional.empty());

        assertThat(clienteService.actualizarCliente(8, actualizacion())).isNotNull();
    }

    @Test
    void actualizarCliente_sinDomicilioRegistrado_lanzaError404() {
        Cliente cliente = clienteConId(8);
        when(clienteRepository.findById(8)).thenReturn(Optional.of(cliente));
        when(clienteRepository.findByCorreoElectronico("nuevo@correo.com")).thenReturn(Optional.empty());
        when(nacionalidadService.buscarPorId(1)).thenReturn(new Nacionalidad());
        when(domicilioRepository.findByClienteId(8)).thenReturn(Optional.empty());

        var cambios = actualizacion();
        assertThatThrownBy(() -> clienteService.actualizarCliente(8, cambios))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void crearCliente_numeroDeCuentaGeneradoYaExiste_generaOtro() {
        ClienteRequest request = new ClienteRequest();
        request.setCurp("CURP-NUEVA");
        request.setRfc("RFC-NUEVO");
        request.setCorreoElectronico("nuevo@correo.com");
        request.setPassword("contrasena123");
        request.setNacionalidadId(1);
        request.setDomicilio(new DomicilioRequest());
        Cliente clienteMapeado = new Cliente();
        Cliente guardado = clienteConId(9);
        when(clienteMapper.toEntity(request)).thenReturn(clienteMapeado);
        when(nacionalidadService.buscarPorId(1)).thenReturn(new Nacionalidad());
        when(clienteRepository.save(clienteMapeado)).thenReturn(guardado);
        when(clienteMapper.toEntity(request.getDomicilio())).thenReturn(new Domicilio());
        when(cuentaRepository.existsByNumeroCuenta(any())).thenReturn(true, false);
        respuestaArmadaPara(guardado);

        clienteService.crearCliente(request);

        verify(cuentaRepository, times(2)).existsByNumeroCuenta(any());
    }
}
