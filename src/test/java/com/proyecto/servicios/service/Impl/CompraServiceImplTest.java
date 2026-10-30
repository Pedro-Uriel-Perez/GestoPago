package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoSenTxClient;
import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.entity.clientes.Cuenta;
import com.proyecto.servicios.entity.clientes.Saldo;
import com.proyecto.servicios.entity.compras.ConciliacionPendiente;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.exception.ClienteInactivoException;
import com.proyecto.servicios.exception.CodigoVerificacionInvalidoException;
import com.proyecto.servicios.exception.CompraPendienteExistenteException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.DatosCompraInvalidosException;
import com.proyecto.servicios.exception.GestoPagoSenTxRechazadoException;
import com.proyecto.servicios.exception.OperacionCompraNoEncontradaException;
import com.proyecto.servicios.exception.OperacionNoAutorizadaException;
import com.proyecto.servicios.exception.ProductoGestoPagoNoEncontradoException;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.exception.TransaccionGestoPagoIndeterminadaException;
import com.proyecto.servicios.model.compras.CompraConfirmarRequest;
import com.proyecto.servicios.model.compras.CompraIniciadaResponse;
import com.proyecto.servicios.model.compras.CompraPendiente;
import com.proyecto.servicios.model.compras.CompraRequest;
import com.proyecto.servicios.model.compras.CompraResultadoResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoSenTxResponse;
import com.proyecto.servicios.model.productlist.MensajeExternoDTO;
import com.proyecto.servicios.repositorys.clientes.CuentaRepository;
import com.proyecto.servicios.repositorys.clientes.SaldoRepository;
import com.proyecto.servicios.repositorys.compras.ConciliacionPendienteRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.security.GestoPagoAesCipher;
import com.proyecto.servicios.service.MailService;
import feign.FeignException;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    private static final Integer CLIENTE_ID = 10;
    private static final Integer CUENTA_ID = 5;
    private static final String OPERACION_ID = "abc";

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private SaldoRepository saldoRepository;

    @Mock
    private GestoPagoProductoRepository productoRepository;

    @Mock
    private ConciliacionPendienteRepository conciliacionRepository;

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
                conciliacionRepository, senTxClient, cipher, mailService, redisTemplate, objectMapper);
        ReflectionTestUtils.setField(compraService, "unidad", "083");
    }

    // ---------- helpers ----------

    private Cliente clienteActivo() {
        Cliente cliente = new Cliente();
        cliente.setId(CLIENTE_ID);
        cliente.setActivo(true);
        cliente.setCorreoElectronico("cliente@correo.com");
        return cliente;
    }

    private Cuenta cuentaDe(Cliente cliente) {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(CUENTA_ID);
        cuenta.setNumeroCuenta("1234567890");
        cuenta.setCliente(cliente);
        return cuenta;
    }

    private GestoPagoProducto productoRecargaDe10Pesos() {
        GestoPagoProducto producto = new GestoPagoProducto();
        producto.setIdProducto(582);
        producto.setIdServicio(133);
        producto.setTipoFront(1);
        producto.setPrecio("10.0");
        return producto;
    }

    private Saldo saldoDe(String monto) {
        Saldo saldo = new Saldo();
        saldo.setMonto(new BigDecimal(monto));
        return saldo;
    }

    private CompraRequest requestRecarga(BigDecimal monto) {
        CompraRequest request = new CompraRequest();
        request.setNumeroCuenta("1234567890");
        request.setIdProducto(582);
        request.setMonto(monto);
        request.setTelefono("5555555551");
        return request;
    }

    private CompraConfirmarRequest confirmarConPin(String pin) {
        CompraConfirmarRequest request = new CompraConfirmarRequest();
        request.setOperacionId(OPERACION_ID);
        request.setCodigoVerificacion(pin);
        return request;
    }

    private void prepararPendienteEnRedis() throws Exception {
        CompraPendiente pendiente = new CompraPendiente(CLIENTE_ID, CUENTA_ID, 582, 133, "5555555551", null, null,
                "UPC123", "083", "20220101235900", BigDecimal.TEN, "111111");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("compra:pendiente:" + OPERACION_ID)).thenReturn(objectMapper.writeValueAsString(pendiente));
    }

    private GestoPagoSenTxResponse respuestaGestoPago(String codigo, String texto) {
        MensajeExternoDTO mensaje = new MensajeExternoDTO();
        mensaje.setCodigo(codigo);
        mensaje.setTexto(texto);
        GestoPagoSenTxResponse respuesta = new GestoPagoSenTxResponse();
        respuesta.setMensaje(mensaje);
        respuesta.setNumAutorizacion("AUTH123");
        respuesta.setIdTx("999");
        return respuesta;
    }

    private void prepararCuentaBloqueadaConSaldo(String saldo) {
        when(cuentaRepository.findWithLockingById(CUENTA_ID)).thenReturn(Optional.of(cuentaDe(clienteActivo())));
        when(redisTemplate.hasKey("compra:pendiente:" + OPERACION_ID)).thenReturn(true);
        when(saldoRepository.findFirstByCuentaIdOrderByFechaMovimientoDesc(CUENTA_ID))
                .thenReturn(Optional.of(saldoDe(saldo)));
    }

    private void reservaDeCuentaDisponible() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("compra:cuenta-activa:" + CUENTA_ID), anyString(), any())).thenReturn(true);
    }

    // ---------- iniciarCompra ----------

    @Test
    void iniciarCompra_cuentaDeOtroCliente_lanzaOperacionNoAutorizadaException() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaDe(clienteActivo())));

        assertThatThrownBy(() -> compraService.iniciarCompra(requestRecarga(BigDecimal.TEN), 999))
                .isInstanceOf(OperacionNoAutorizadaException.class);

        verify(mailService, never()).enviarCodigoVerificacion(any(), any());
    }

    @Test
    void iniciarCompra_clienteInactivo_lanzaClienteInactivoException() {
        Cliente cliente = clienteActivo();
        cliente.setActivo(false);
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaDe(cliente)));

        assertThatThrownBy(() -> compraService.iniciarCompra(requestRecarga(BigDecimal.TEN), CLIENTE_ID))
                .isInstanceOf(ClienteInactivoException.class);

        verify(productoRepository, never()).findByIdProducto(any());
    }

    @Test
    void iniciarCompra_cuentaNoExiste_lanzaCuentaNoEncontradaException() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compraService.iniciarCompra(requestRecarga(BigDecimal.TEN), CLIENTE_ID))
                .isInstanceOf(CuentaNoEncontradaException.class);
    }

    @Test
    void iniciarCompra_yaHayUnaCompraPendienteEnLaCuenta_lanzaCompraPendienteExistenteException() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaDe(clienteActivo())));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("compra:cuenta-activa:" + CUENTA_ID), anyString(), any())).thenReturn(false);

        assertThatThrownBy(() -> compraService.iniciarCompra(requestRecarga(BigDecimal.TEN), CLIENTE_ID))
                .isInstanceOf(CompraPendienteExistenteException.class);

        verify(productoRepository, never()).findByIdProducto(any());
        verify(mailService, never()).enviarCodigoVerificacion(any(), any());
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void iniciarCompra_productoNoExiste_lanzaExcepcionYLiberaLaReservaDeLaCuenta() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaDe(clienteActivo())));
        reservaDeCuentaDisponible();
        when(productoRepository.findByIdProducto(582)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compraService.iniciarCompra(requestRecarga(BigDecimal.TEN), CLIENTE_ID))
                .isInstanceOf(ProductoGestoPagoNoEncontradoException.class);

        verify(redisTemplate).delete("compra:cuenta-activa:" + CUENTA_ID);
    }

    @Test
    void iniciarCompra_montoDistintoAlPrecioDelCatalogo_lanzaDatosCompraInvalidosException() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaDe(clienteActivo())));
        reservaDeCuentaDisponible();
        when(productoRepository.findByIdProducto(582)).thenReturn(Optional.of(productoRecargaDe10Pesos()));

        assertThatThrownBy(() -> compraService.iniciarCompra(requestRecarga(new BigDecimal("1.00")), CLIENTE_ID))
                .isInstanceOf(DatosCompraInvalidosException.class)
                .hasMessageContaining("catalogo");

        verify(saldoRepository, never()).findFirstByCuentaIdOrderByFechaMovimientoDesc(any());
        verify(redisTemplate).delete("compra:cuenta-activa:" + CUENTA_ID);
    }

    @Test
    void iniciarCompra_pagoDeServicio_aceptaMontoLibreSinCompararConCatalogo() {
        GestoPagoProducto servicio = productoRecargaDe10Pesos();
        servicio.setTipoFront(2);
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaDe(clienteActivo())));
        reservaDeCuentaDisponible();
        when(productoRepository.findByIdProducto(582)).thenReturn(Optional.of(servicio));
        when(saldoRepository.findFirstByCuentaIdOrderByFechaMovimientoDesc(CUENTA_ID))
                .thenReturn(Optional.of(saldoDe("1000.00")));

        CompraRequest request = requestRecarga(new BigDecimal("357.40"));
        request.setTelefono(null);
        request.setReferencia("40425475190118187271");

        CompraIniciadaResponse resultado = compraService.iniciarCompra(request, CLIENTE_ID);

        assertThat(resultado.getOperacionId()).isNotBlank();
    }

    @Test
    void iniciarCompra_saldoInsuficiente_lanzaExcepcionYLiberaLaReservaDeLaCuenta() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaDe(clienteActivo())));
        reservaDeCuentaDisponible();
        when(productoRepository.findByIdProducto(582)).thenReturn(Optional.of(productoRecargaDe10Pesos()));
        when(saldoRepository.findFirstByCuentaIdOrderByFechaMovimientoDesc(CUENTA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compraService.iniciarCompra(requestRecarga(BigDecimal.TEN), CLIENTE_ID))
                .isInstanceOf(SaldoInsuficienteException.class);

        verify(mailService, never()).enviarCodigoVerificacion(any(), any());
        verify(redisTemplate).delete("compra:cuenta-activa:" + CUENTA_ID);
    }

    @Test
    void iniciarCompra_productoTipoFront1SinTelefono_lanzaDatosCompraInvalidosException() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaDe(clienteActivo())));
        reservaDeCuentaDisponible();
        when(productoRepository.findByIdProducto(582)).thenReturn(Optional.of(productoRecargaDe10Pesos()));
        when(saldoRepository.findFirstByCuentaIdOrderByFechaMovimientoDesc(CUENTA_ID))
                .thenReturn(Optional.of(saldoDe("100.00")));

        CompraRequest request = requestRecarga(BigDecimal.TEN);
        request.setTelefono(null);

        assertThatThrownBy(() -> compraService.iniciarCompra(request, CLIENTE_ID))
                .isInstanceOf(DatosCompraInvalidosException.class);
    }

    @Test
    void iniciarCompra_datosValidos_reservaLaCuentaGuardaPendienteYMandaCorreo() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaDe(clienteActivo())));
        reservaDeCuentaDisponible();
        when(productoRepository.findByIdProducto(582)).thenReturn(Optional.of(productoRecargaDe10Pesos()));
        when(saldoRepository.findFirstByCuentaIdOrderByFechaMovimientoDesc(CUENTA_ID))
                .thenReturn(Optional.of(saldoDe("100.00")));

        CompraIniciadaResponse resultado = compraService.iniciarCompra(requestRecarga(BigDecimal.TEN), CLIENTE_ID);

        assertThat(resultado.getOperacionId()).isNotBlank();
        verify(valueOperations).setIfAbsent(eq("compra:cuenta-activa:" + CUENTA_ID), eq(resultado.getOperacionId()), any());
        verify(valueOperations).set(eq("compra:pendiente:" + resultado.getOperacionId()), anyString(), any());
        verify(mailService).enviarCodigoVerificacion(eq("cliente@correo.com"), anyString());
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void iniciarCompra_fallaElEnvioDelCorreo_liberaLaReservaParaPoderReintentar() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaDe(clienteActivo())));
        reservaDeCuentaDisponible();
        when(productoRepository.findByIdProducto(582)).thenReturn(Optional.of(productoRecargaDe10Pesos()));
        when(saldoRepository.findFirstByCuentaIdOrderByFechaMovimientoDesc(CUENTA_ID))
                .thenReturn(Optional.of(saldoDe("100.00")));
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("smtp caido"))
                .when(mailService).enviarCodigoVerificacion(anyString(), anyString());

        assertThatThrownBy(() -> compraService.iniciarCompra(requestRecarga(BigDecimal.TEN), CLIENTE_ID))
                .isInstanceOf(org.springframework.mail.MailSendException.class);

        verify(redisTemplate).delete("compra:cuenta-activa:" + CUENTA_ID);
    }

    // ---------- confirmarCompra ----------

    @Test
    void confirmarCompra_operacionNoExiste_lanzaOperacionCompraNoEncontradaException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("compra:pendiente:" + OPERACION_ID)).thenReturn(null);

        assertThatThrownBy(() -> compraService.confirmarCompra(confirmarConPin("123456"), CLIENTE_ID))
                .isInstanceOf(OperacionCompraNoEncontradaException.class);
    }

    @Test
    void confirmarCompra_operacionDeOtroCliente_lanzaOperacionNoAutorizadaException() throws Exception {
        prepararPendienteEnRedis();

        assertThatThrownBy(() -> compraService.confirmarCompra(confirmarConPin("111111"), 999))
                .isInstanceOf(OperacionNoAutorizadaException.class);

        verify(senTxClient, never()).sendTx(any());
    }

    @Test
    void confirmarCompra_codigoIncorrecto_lanzaCodigoVerificacionInvalidoException() throws Exception {
        prepararPendienteEnRedis();

        assertThatThrownBy(() -> compraService.confirmarCompra(confirmarConPin("999999"), CLIENTE_ID))
                .isInstanceOf(CodigoVerificacionInvalidoException.class);

        verify(senTxClient, never()).sendTx(any());
    }

    @Test
    void confirmarCompra_dobleEnvioYaProcesadoPorLaPrimera_noLlamaAGestoPagoDeNuevo() throws Exception {
        prepararPendienteEnRedis();
        when(cuentaRepository.findWithLockingById(CUENTA_ID)).thenReturn(Optional.of(cuentaDe(clienteActivo())));
        when(redisTemplate.hasKey("compra:pendiente:" + OPERACION_ID)).thenReturn(false);

        assertThatThrownBy(() -> compraService.confirmarCompra(confirmarConPin("111111"), CLIENTE_ID))
                .isInstanceOf(OperacionCompraNoEncontradaException.class);

        verify(senTxClient, never()).sendTx(any());
        verify(saldoRepository, never()).save(any());
    }

    @Test
    void confirmarCompra_saldoYaNoAlcanzaAlBloquearLaCuenta_noLlamaAGestoPago() throws Exception {
        prepararPendienteEnRedis();
        prepararCuentaBloqueadaConSaldo("5.00");

        assertThatThrownBy(() -> compraService.confirmarCompra(confirmarConPin("111111"), CLIENTE_ID))
                .isInstanceOf(SaldoInsuficienteException.class);

        verify(senTxClient, never()).sendTx(any());
        verify(saldoRepository, never()).save(any());
        verify(redisTemplate).delete("compra:cuenta-activa:" + CUENTA_ID);
    }

    @Test
    void confirmarCompra_gestoPagoRechaza_lanzaExcepcionYLiberaLaCuenta() throws Exception {
        prepararPendienteEnRedis();
        prepararCuentaBloqueadaConSaldo("100.00");
        when(cipher.encriptar(anyString())).thenReturn("cifrado");
        when(senTxClient.sendTx(any())).thenReturn(respuestaGestoPago("02", "Saldo Insuficiente."));

        assertThatThrownBy(() -> compraService.confirmarCompra(confirmarConPin("111111"), CLIENTE_ID))
                .isInstanceOf(GestoPagoSenTxRechazadoException.class)
                .hasMessage("Saldo Insuficiente.");

        verify(saldoRepository, never()).save(any());
        verify(redisTemplate).delete("compra:pendiente:" + OPERACION_ID);
        verify(redisTemplate).delete("compra:cuenta-activa:" + CUENTA_ID);
    }

    @Test
    void confirmarCompra_exitosa_registraMovimientoYLiberaLaCuenta() throws Exception {
        prepararPendienteEnRedis();
        prepararCuentaBloqueadaConSaldo("100.00");
        when(cipher.encriptar(anyString())).thenReturn("cifrado");
        when(senTxClient.sendTx(any())).thenReturn(respuestaGestoPago("01", "Operacion realizada con exito"));

        CompraResultadoResponse resultado = compraService.confirmarCompra(confirmarConPin("111111"), CLIENTE_ID);

        assertThat(resultado.getNumeroAutorizacion()).isEqualTo("AUTH123");
        assertThat(resultado.getSaldoRestante()).isEqualTo("90.00");
        ArgumentCaptor<Saldo> movimiento = ArgumentCaptor.forClass(Saldo.class);
        verify(saldoRepository).save(movimiento.capture());
        assertThat(movimiento.getValue().getTipoMovimiento()).isEqualTo("COMPRA");
        verify(redisTemplate).delete("compra:pendiente:" + OPERACION_ID);
        verify(redisTemplate).delete("compra:cuenta-activa:" + CUENTA_ID);
        verify(senTxClient, never()).confirmTx(any());
    }

    @Test
    void confirmarCompra_timeoutEnSendTx_consultaConfirmTxYSiFueExitosaDescuentaSaldo() throws Exception {
        prepararPendienteEnRedis();
        prepararCuentaBloqueadaConSaldo("100.00");
        when(cipher.encriptar(anyString())).thenReturn("cifrado");
        when(senTxClient.sendTx(any())).thenThrow(RetryableException.class);
        when(senTxClient.confirmTx(any())).thenReturn(respuestaGestoPago("01", "Operacion realizada con exito"));

        CompraResultadoResponse resultado = compraService.confirmarCompra(confirmarConPin("111111"), CLIENTE_ID);

        assertThat(resultado.getSaldoRestante()).isEqualTo("90.00");
        verify(senTxClient).confirmTx(any());
        verify(saldoRepository).save(any());
    }

    @Test
    void confirmarCompra_errorDeServidorEnSendTx_tambienConsultaConfirmTx() throws Exception {
        prepararPendienteEnRedis();
        prepararCuentaBloqueadaConSaldo("100.00");
        when(cipher.encriptar(anyString())).thenReturn("cifrado");
        when(senTxClient.sendTx(any())).thenThrow(FeignException.InternalServerError.class);
        when(senTxClient.confirmTx(any())).thenReturn(respuestaGestoPago("01", "Operacion realizada con exito"));

        compraService.confirmarCompra(confirmarConPin("111111"), CLIENTE_ID);

        verify(senTxClient).confirmTx(any());
    }

    @Test
    void confirmarCompra_timeoutEnSendTxYConfirmTxDiceQueNoSeAplico_noDescuentaSaldo() throws Exception {
        prepararPendienteEnRedis();
        prepararCuentaBloqueadaConSaldo("100.00");
        when(cipher.encriptar(anyString())).thenReturn("cifrado");
        when(senTxClient.sendTx(any())).thenThrow(RetryableException.class);
        when(senTxClient.confirmTx(any())).thenReturn(respuestaGestoPago("82", "La transaccion no ha sido registrada"));

        assertThatThrownBy(() -> compraService.confirmarCompra(confirmarConPin("111111"), CLIENTE_ID))
                .isInstanceOf(GestoPagoSenTxRechazadoException.class);

        verify(saldoRepository, never()).save(any());
    }

    @Test
    void confirmarCompra_timeoutEnSendTxYConfirmTx_registraConciliacionYNoLiberaLaCuenta() throws Exception {
        prepararPendienteEnRedis();
        prepararCuentaBloqueadaConSaldo("100.00");
        when(cipher.encriptar(anyString())).thenReturn("cifrado");
        when(senTxClient.sendTx(any())).thenThrow(RetryableException.class);
        when(senTxClient.confirmTx(any())).thenThrow(RetryableException.class);

        assertThatThrownBy(() -> compraService.confirmarCompra(confirmarConPin("111111"), CLIENTE_ID))
                .isInstanceOf(TransaccionGestoPagoIndeterminadaException.class);

        ArgumentCaptor<ConciliacionPendiente> conciliacion = ArgumentCaptor.forClass(ConciliacionPendiente.class);
        verify(conciliacionRepository).save(conciliacion.capture());
        assertThat(conciliacion.getValue().getUpc()).isEqualTo("UPC123");
        assertThat(conciliacion.getValue().getMonto()).isEqualByComparingTo(BigDecimal.TEN);
        verify(saldoRepository, never()).save(any());
        verify(redisTemplate, never()).delete(anyString());
    }
}
