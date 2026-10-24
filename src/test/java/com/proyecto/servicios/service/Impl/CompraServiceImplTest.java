package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoSenTxClient;
import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.entity.clientes.Cuenta;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.exception.ClienteInactivoException;
import com.proyecto.servicios.exception.CodigoVerificacionInvalidoException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.GestoPagoSenTxRechazadoException;
import com.proyecto.servicios.exception.OperacionCompraNoEncontradaException;
import com.proyecto.servicios.exception.ProductoGestoPagoNoEncontradoException;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.model.compras.CompraConfirmarRequest;
import com.proyecto.servicios.model.compras.CompraIniciadaResponse;
import com.proyecto.servicios.model.compras.CompraPendiente;
import com.proyecto.servicios.model.compras.CompraRequest;
import com.proyecto.servicios.model.compras.CompraResultadoResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoSenTxResponse;
import com.proyecto.servicios.model.productlist.MensajeExternoDTO;
import com.proyecto.servicios.repositorys.clientes.CuentaRepository;
import com.proyecto.servicios.repositorys.clientes.SaldoRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.security.GestoPagoAesCipher;
import com.proyecto.servicios.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompraServiceImplTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private SaldoRepository saldoRepository;

    @Mock
    private GestoPagoProductoRepository productoRepository;

    @Mock
    private GestoPagoSenTxClient senTxClient;

    @Mock
    private GestoPagoAesCipher cipher;

    @Mock
    private MailService mailService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CompraServiceImpl compraService;

    @BeforeEach
    void setUp() {
        compraService = new CompraServiceImpl(cuentaRepository, saldoRepository, productoRepository,
                senTxClient, cipher, mailService, redisTemplate, objectMapper);
        ReflectionTestUtils.setField(compraService, "unidad", "083");
    }

    private Cuenta cuentaDe(Cliente cliente) {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(5);
        cuenta.setNumeroCuenta("1234567890");
        cuenta.setCliente(cliente);
        return cuenta;
    }

    private Cliente clienteActivo() {
        Cliente cliente = new Cliente();
        cliente.setId(10);
        cliente.setActivo(true);
        cliente.setCorreoElectronico("cliente@correo.com");
        return cliente;
    }

    private GestoPagoProducto productoTipoFront1() {
        GestoPagoProducto producto = new GestoPagoProducto();
        producto.setIdProducto(582);
        producto.setIdServicio(133);
        producto.setTipoFront(1);
        return producto;
    }

    @Test
    void iniciarCompra_clienteInactivo_lanzaClienteInactivoException() {
        Cliente cliente = clienteActivo();
        cliente.setActivo(false);
        Cuenta cuenta = cuentaDe(cliente);
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta));

        CompraRequest request = new CompraRequest();
        request.setNumeroCuenta("1234567890");
        request.setIdProducto(582);
        request.setMonto(BigDecimal.TEN);

        assertThatThrownBy(() -> compraService.iniciarCompra(request))
                .isInstanceOf(ClienteInactivoException.class);

        verify(productoRepository, never()).findByIdProducto(any());
    }

    @Test
    void iniciarCompra_cuentaNoExiste_lanzaCuentaNoEncontradaException() {
        when(cuentaRepository.findByNumeroCuenta("0000000000")).thenReturn(Optional.empty());

        CompraRequest request = new CompraRequest();
        request.setNumeroCuenta("0000000000");
        request.setIdProducto(582);
        request.setMonto(BigDecimal.TEN);

        assertThatThrownBy(() -> compraService.iniciarCompra(request))
                .isInstanceOf(CuentaNoEncontradaException.class);
    }

    @Test
    void iniciarCompra_productoNoExiste_lanzaProductoGestoPagoNoEncontradoException() {
        Cliente cliente = clienteActivo();
        Cuenta cuenta = cuentaDe(cliente);
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta));
        when(productoRepository.findByIdProducto(999)).thenReturn(Optional.empty());

        CompraRequest request = new CompraRequest();
        request.setNumeroCuenta("1234567890");
        request.setIdProducto(999);
        request.setMonto(BigDecimal.TEN);

        assertThatThrownBy(() -> compraService.iniciarCompra(request))
                .isInstanceOf(ProductoGestoPagoNoEncontradoException.class);
    }

    @Test
    void iniciarCompra_saldoInsuficiente_lanzaSaldoInsuficienteException() {
        Cliente cliente = clienteActivo();
        Cuenta cuenta = cuentaDe(cliente);
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta));
        when(productoRepository.findByIdProducto(582)).thenReturn(Optional.of(productoTipoFront1()));
        when(saldoRepository.findFirstByCuentaIdOrderByFechaMovimientoDesc(5)).thenReturn(Optional.empty());

        CompraRequest request = new CompraRequest();
        request.setNumeroCuenta("1234567890");
        request.setIdProducto(582);
        request.setMonto(BigDecimal.TEN);
        request.setTelefono("5555555551");

        assertThatThrownBy(() -> compraService.iniciarCompra(request))
                .isInstanceOf(SaldoInsuficienteException.class);

        verify(mailService, never()).enviarCodigoVerificacion(any(), any());
    }

    @Test
    void iniciarCompra_datosValidos_guardaPendienteYMandaCorreo() {
        Cliente cliente = clienteActivo();
        Cuenta cuenta = cuentaDe(cliente);
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta));
        when(productoRepository.findByIdProducto(582)).thenReturn(Optional.of(productoTipoFront1()));
        com.proyecto.servicios.entity.clientes.Saldo saldo = new com.proyecto.servicios.entity.clientes.Saldo();
        saldo.setMonto(new BigDecimal("100.00"));
        when(saldoRepository.findFirstByCuentaIdOrderByFechaMovimientoDesc(5)).thenReturn(Optional.of(saldo));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        CompraRequest request = new CompraRequest();
        request.setNumeroCuenta("1234567890");
        request.setIdProducto(582);
        request.setMonto(BigDecimal.TEN);
        request.setTelefono("5555555551");

        CompraIniciadaResponse resultado = compraService.iniciarCompra(request);

        assertThat(resultado.getOperacionId()).isNotBlank();
        verify(valueOperations).set(anyString(), anyString(), any());
        verify(mailService).enviarCodigoVerificacion(eq("cliente@correo.com"), anyString());
    }

    @Test
    void iniciarCompra_productoTipoFront1SinTelefono_lanzaDatosCompraInvalidosException() {
        Cliente cliente = clienteActivo();
        Cuenta cuenta = cuentaDe(cliente);
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta));
        when(productoRepository.findByIdProducto(582)).thenReturn(Optional.of(productoTipoFront1()));
        com.proyecto.servicios.entity.clientes.Saldo saldo = new com.proyecto.servicios.entity.clientes.Saldo();
        saldo.setMonto(new BigDecimal("100.00"));
        when(saldoRepository.findFirstByCuentaIdOrderByFechaMovimientoDesc(5)).thenReturn(Optional.of(saldo));

        CompraRequest request = new CompraRequest();
        request.setNumeroCuenta("1234567890");
        request.setIdProducto(582);
        request.setMonto(BigDecimal.TEN);

        assertThatThrownBy(() -> compraService.iniciarCompra(request))
                .isInstanceOf(com.proyecto.servicios.exception.DatosCompraInvalidosException.class);
    }

    @Test
    void confirmarCompra_operacionNoExiste_lanzaOperacionCompraNoEncontradaException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("compra:pendiente:abc")).thenReturn(null);

        CompraConfirmarRequest request = new CompraConfirmarRequest();
        request.setOperacionId("abc");
        request.setCodigoVerificacion("123456");

        assertThatThrownBy(() -> compraService.confirmarCompra(request))
                .isInstanceOf(OperacionCompraNoEncontradaException.class);
    }

    @Test
    void confirmarCompra_codigoIncorrecto_lanzaCodigoVerificacionInvalidoException() throws Exception {
        CompraPendiente pendiente = new CompraPendiente(10, 5, 582, 133, "5555555551", null, null,
                "UPC123", "083", "20220101235900", BigDecimal.TEN, "111111");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("compra:pendiente:abc")).thenReturn(objectMapper.writeValueAsString(pendiente));

        CompraConfirmarRequest request = new CompraConfirmarRequest();
        request.setOperacionId("abc");
        request.setCodigoVerificacion("999999");

        assertThatThrownBy(() -> compraService.confirmarCompra(request))
                .isInstanceOf(CodigoVerificacionInvalidoException.class);

        verify(senTxClient, never()).sendTx(any());
    }

    @Test
    void confirmarCompra_gestoPagoRechaza_lanzaGestoPagoSenTxRechazadoException() throws Exception {
        CompraPendiente pendiente = new CompraPendiente(10, 5, 582, 133, "5555555551", null, null,
                "UPC123", "083", "20220101235900", BigDecimal.TEN, "111111");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("compra:pendiente:abc")).thenReturn(objectMapper.writeValueAsString(pendiente));
        when(cipher.encriptar(anyString())).thenReturn("cifrado");

        MensajeExternoDTO mensaje = new MensajeExternoDTO();
        mensaje.setCodigo("02");
        mensaje.setTexto("Saldo Insuficiente.");
        GestoPagoSenTxResponse respuesta = new GestoPagoSenTxResponse();
        respuesta.setMensaje(mensaje);
        when(senTxClient.sendTx(any())).thenReturn(respuesta);

        CompraConfirmarRequest request = new CompraConfirmarRequest();
        request.setOperacionId("abc");
        request.setCodigoVerificacion("111111");

        assertThatThrownBy(() -> compraService.confirmarCompra(request))
                .isInstanceOf(GestoPagoSenTxRechazadoException.class)
                .hasMessage("Saldo Insuficiente.");

        verify(saldoRepository, never()).save(any());
    }

    @Test
    void confirmarCompra_exitosa_registraMovimientoYBorraPendiente() throws Exception {
        CompraPendiente pendiente = new CompraPendiente(10, 5, 582, 133, "5555555551", null, null,
                "UPC123", "083", "20220101235900", BigDecimal.TEN, "111111");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("compra:pendiente:abc")).thenReturn(objectMapper.writeValueAsString(pendiente));
        when(cipher.encriptar(anyString())).thenReturn("cifrado");

        MensajeExternoDTO mensaje = new MensajeExternoDTO();
        mensaje.setCodigo("01");
        mensaje.setTexto("Operacion realizada con exito");
        GestoPagoSenTxResponse respuesta = new GestoPagoSenTxResponse();
        respuesta.setMensaje(mensaje);
        respuesta.setNumAutorizacion("AUTH123");
        respuesta.setIdTx("999");
        when(senTxClient.sendTx(any())).thenReturn(respuesta);

        Cuenta cuenta = cuentaDe(clienteActivo());
        when(cuentaRepository.findById(5)).thenReturn(Optional.of(cuenta));
        com.proyecto.servicios.entity.clientes.Saldo saldoPrevio = new com.proyecto.servicios.entity.clientes.Saldo();
        saldoPrevio.setMonto(new BigDecimal("100.00"));
        when(saldoRepository.findFirstByCuentaIdOrderByFechaMovimientoDesc(5)).thenReturn(Optional.of(saldoPrevio));

        CompraConfirmarRequest request = new CompraConfirmarRequest();
        request.setOperacionId("abc");
        request.setCodigoVerificacion("111111");

        CompraResultadoResponse resultado = compraService.confirmarCompra(request);

        assertThat(resultado.getNumeroAutorizacion()).isEqualTo("AUTH123");
        assertThat(resultado.getSaldoRestante()).isEqualTo("90.00");
        verify(saldoRepository).save(any());
        verify(redisTemplate).delete("compra:pendiente:abc");
    }
}
