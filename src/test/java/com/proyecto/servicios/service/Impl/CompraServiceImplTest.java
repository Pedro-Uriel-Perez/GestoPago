package com.proyecto.servicios.service.Impl;

import org.springframework.http.HttpStatus;
import com.proyecto.servicios.exception.NegocioException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoSenTxClient;
import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.entity.clientes.Cuenta;
import com.proyecto.servicios.entity.clientes.Saldo;
import com.proyecto.servicios.config.GestoPagoCatalogo;
import com.proyecto.servicios.entity.compras.ConciliacionPendiente;
import com.proyecto.servicios.entity.compras.Pago;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.model.compras.CompraConfirmarRequest;
import com.proyecto.servicios.model.compras.CompraIniciadaResponse;
import com.proyecto.servicios.model.compras.CompraPendiente;
import com.proyecto.servicios.model.compras.CompraRequest;
import com.proyecto.servicios.model.compras.CompraResultadoResponse;
import com.proyecto.servicios.model.compras.PagoResponse;
import com.proyecto.servicios.model.compras.ReferenciaVerificadaResponse;
import com.proyecto.servicios.model.compras.VerificarReferenciaRequest;
import com.proyecto.servicios.model.gestopago.GestoPagoSenTxResponse;
import com.proyecto.servicios.model.productlist.MensajeExternoDTO;
import com.proyecto.servicios.repositorys.clientes.CuentaRepository;
import com.proyecto.servicios.repositorys.clientes.SaldoRepository;
import com.proyecto.servicios.repositorys.compras.ConciliacionPendienteRepository;
import com.proyecto.servicios.repositorys.compras.PagoRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
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
    private PagoRepository pagoRepository;

    @Mock
    private EvidenciaGestoPago bitacora;

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
                conciliacionRepository, pagoRepository, new GestoPagoCatalogo("https://imagenes.test"),
                bitacora, senTxClient, cipher, mailService, redisTemplate, objectMapper);
        lenient().when(bitacora.registrar(anyString(), any(), any(), any())).thenAnswer(inv ->
                inv.<java.util.function.Supplier<GestoPagoSenTxResponse>>getArgument(3).get());
        ReflectionTestUtils.setField(compraService, "unidad", "083");
        ReflectionTestUtils.setField(compraService, "exigirValidacionDeReferencia", true);
        lenient().when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago pago = inv.getArgument(0);
            pago.setId(77);
            return pago;
        });
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
        when(saldoRepository.findFirstByCuentaIdOrderByIdDesc(CUENTA_ID))
                .thenReturn(Optional.of(saldoDe(saldo)));
    }

    private void reservaDeCuentaDisponible() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("compra:cuenta-activa:" + CUENTA_ID), anyString(), any())).thenReturn(true);
    }

    // ---------- iniciarCompra ----------

    @Test
    void iniciarCompra_cuentaDeOtroCliente_lanzaError403() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaDe(clienteActivo())));

        CompraRequest solicitud = requestRecarga(BigDecimal.TEN);
        assertThatThrownBy(() -> compraService.iniciarCompra(solicitud, 999))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        verify(mailService, never()).enviarCodigoVerificacion(any(), any());
    }

    @Test
    void iniciarCompra_clienteInactivo_lanzaError403() {
        Cliente cliente = clienteActivo();
        cliente.setActivo(false);
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaDe(cliente)));

        CompraRequest solicitud = requestRecarga(BigDecimal.TEN);
        assertThatThrownBy(() -> compraService.iniciarCompra(solicitud, CLIENTE_ID))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        verify(productoRepository, never()).findByIdProducto(any());
    }

    @Test
    void iniciarCompra_cuentaNoExiste_lanzaError404() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.empty());

        CompraRequest solicitud = requestRecarga(BigDecimal.TEN);
        assertThatThrownBy(() -> compraService.iniciarCompra(solicitud, CLIENTE_ID))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void iniciarCompra_yaHayUnaCompraPendienteEnLaCuenta_lanzaError409() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaDe(clienteActivo())));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("compra:cuenta-activa:" + CUENTA_ID), anyString(), any())).thenReturn(false);

        CompraRequest solicitud = requestRecarga(BigDecimal.TEN);
        assertThatThrownBy(() -> compraService.iniciarCompra(solicitud, CLIENTE_ID))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

        verify(productoRepository, never()).findByIdProducto(any());
        verify(mailService, never()).enviarCodigoVerificacion(any(), any());
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void iniciarCompra_productoNoExiste_lanzaExcepcionYLiberaLaReservaDeLaCuenta() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaDe(clienteActivo())));
        reservaDeCuentaDisponible();
        when(productoRepository.findByIdProducto(582)).thenReturn(Optional.empty());

        CompraRequest solicitud = requestRecarga(BigDecimal.TEN);
        assertThatThrownBy(() -> compraService.iniciarCompra(solicitud, CLIENTE_ID))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

        verify(redisTemplate).delete("compra:cuenta-activa:" + CUENTA_ID);
    }

    @Test
    void iniciarCompra_montoDistintoAlPrecioDelCatalogo_lanzaError400() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaDe(clienteActivo())));
        reservaDeCuentaDisponible();
        when(productoRepository.findByIdProducto(582)).thenReturn(Optional.of(productoRecargaDe10Pesos()));

        CompraRequest solicitud = requestRecarga(new BigDecimal("1.00"));
        assertThatThrownBy(() -> compraService.iniciarCompra(solicitud, CLIENTE_ID))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("catalogo");

        verify(saldoRepository, never()).findFirstByCuentaIdOrderByIdDesc(any());
        verify(redisTemplate).delete("compra:cuenta-activa:" + CUENTA_ID);
    }

    @Test
    void iniciarCompra_pagoDeServicio_aceptaMontoLibreSinCompararConCatalogo() {
        GestoPagoProducto servicio = productoRecargaDe10Pesos();
        servicio.setTipoFront(2);
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaDe(clienteActivo())));
        reservaDeCuentaDisponible();
        when(productoRepository.findByIdProducto(582)).thenReturn(Optional.of(servicio));
        when(saldoRepository.findFirstByCuentaIdOrderByIdDesc(CUENTA_ID))
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
        when(saldoRepository.findFirstByCuentaIdOrderByIdDesc(CUENTA_ID)).thenReturn(Optional.empty());

        CompraRequest solicitud = requestRecarga(BigDecimal.TEN);
        assertThatThrownBy(() -> compraService.iniciarCompra(solicitud, CLIENTE_ID))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

        verify(mailService, never()).enviarCodigoVerificacion(any(), any());
        verify(redisTemplate).delete("compra:cuenta-activa:" + CUENTA_ID);
    }

    @Test
    void iniciarCompra_productoTipoFront1SinTelefono_lanzaError400() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaDe(clienteActivo())));
        reservaDeCuentaDisponible();
        when(productoRepository.findByIdProducto(582)).thenReturn(Optional.of(productoRecargaDe10Pesos()));
        when(saldoRepository.findFirstByCuentaIdOrderByIdDesc(CUENTA_ID))
                .thenReturn(Optional.of(saldoDe("100.00")));

        CompraRequest request = requestRecarga(BigDecimal.TEN);
        request.setTelefono(null);

        assertThatThrownBy(() -> compraService.iniciarCompra(request, CLIENTE_ID))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    void iniciarCompra_datosValidos_reservaLaCuentaGuardaPendienteYMandaCorreo() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaDe(clienteActivo())));
        reservaDeCuentaDisponible();
        when(productoRepository.findByIdProducto(582)).thenReturn(Optional.of(productoRecargaDe10Pesos()));
        when(saldoRepository.findFirstByCuentaIdOrderByIdDesc(CUENTA_ID))
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
        when(saldoRepository.findFirstByCuentaIdOrderByIdDesc(CUENTA_ID))
                .thenReturn(Optional.of(saldoDe("100.00")));
        doThrow(new org.springframework.mail.MailSendException("smtp caido"))
                .when(mailService).enviarCodigoVerificacion(anyString(), anyString());

        CompraRequest solicitud = requestRecarga(BigDecimal.TEN);
        assertThatThrownBy(() -> compraService.iniciarCompra(solicitud, CLIENTE_ID))
                .isInstanceOf(org.springframework.mail.MailSendException.class);

        verify(redisTemplate).delete("compra:cuenta-activa:" + CUENTA_ID);
    }

    // ---------- confirmarCompra ----------

    @Test
    void confirmarCompra_operacionNoExiste_lanzaError404() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("compra:pendiente:" + OPERACION_ID)).thenReturn(null);

        CompraConfirmarRequest confirmacion = confirmarConPin("123456");
        assertThatThrownBy(() -> compraService.confirmarCompra(confirmacion, CLIENTE_ID))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void confirmarCompra_operacionDeOtroCliente_lanzaError403() throws Exception {
        prepararPendienteEnRedis();

        CompraConfirmarRequest confirmacion = confirmarConPin("111111");
        assertThatThrownBy(() -> compraService.confirmarCompra(confirmacion, 999))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        verify(senTxClient, never()).sendTx(any());
    }

    @Test
    void confirmarCompra_codigoIncorrecto_lanzaError401() throws Exception {
        prepararPendienteEnRedis();

        CompraConfirmarRequest confirmacion = confirmarConPin("999999");
        assertThatThrownBy(() -> compraService.confirmarCompra(confirmacion, CLIENTE_ID))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);

        verify(senTxClient, never()).sendTx(any());
    }

    @Test
    void confirmarCompra_dobleEnvioYaProcesadoPorLaPrimera_noLlamaAGestoPagoDeNuevo() throws Exception {
        prepararPendienteEnRedis();
        when(cuentaRepository.findWithLockingById(CUENTA_ID)).thenReturn(Optional.of(cuentaDe(clienteActivo())));
        when(redisTemplate.hasKey("compra:pendiente:" + OPERACION_ID)).thenReturn(false);

        CompraConfirmarRequest confirmacion = confirmarConPin("111111");
        assertThatThrownBy(() -> compraService.confirmarCompra(confirmacion, CLIENTE_ID))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

        verify(senTxClient, never()).sendTx(any());
        verify(saldoRepository, never()).save(any());
    }

    @Test
    void confirmarCompra_saldoYaNoAlcanzaAlBloquearLaCuenta_noLlamaAGestoPago() throws Exception {
        prepararPendienteEnRedis();
        prepararCuentaBloqueadaConSaldo("5.00");

        CompraConfirmarRequest confirmacion = confirmarConPin("111111");
        assertThatThrownBy(() -> compraService.confirmarCompra(confirmacion, CLIENTE_ID))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

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

        CompraConfirmarRequest confirmacion = confirmarConPin("111111");
        assertThatThrownBy(() -> compraService.confirmarCompra(confirmacion, CLIENTE_ID))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
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

        CompraConfirmarRequest confirmacion = confirmarConPin("111111");
        assertThatThrownBy(() -> compraService.confirmarCompra(confirmacion, CLIENTE_ID))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

        verify(saldoRepository, never()).save(any());
    }

    @Test
    void confirmarCompra_timeoutEnSendTxYConfirmTx_registraConciliacionYNoLiberaLaCuenta() throws Exception {
        prepararPendienteEnRedis();
        prepararCuentaBloqueadaConSaldo("100.00");
        when(cipher.encriptar(anyString())).thenReturn("cifrado");
        when(senTxClient.sendTx(any())).thenThrow(RetryableException.class);
        when(senTxClient.confirmTx(any())).thenThrow(RetryableException.class);

        CompraConfirmarRequest confirmacion = confirmarConPin("111111");
        assertThatThrownBy(() -> compraService.confirmarCompra(confirmacion, CLIENTE_ID))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.ACCEPTED);

        ArgumentCaptor<ConciliacionPendiente> conciliacion = ArgumentCaptor.forClass(ConciliacionPendiente.class);
        verify(conciliacionRepository).save(conciliacion.capture());
        assertThat(conciliacion.getValue().getUpc()).isEqualTo("UPC123");
        assertThat(conciliacion.getValue().getMonto()).isEqualByComparingTo(BigDecimal.TEN);
        verify(saldoRepository, never()).save(any());
        verify(redisTemplate, never()).delete(anyString());
    }

    // ---------- historial de pagos ----------

    private Pago pagoGuardado() {
        ArgumentCaptor<Pago> pago = ArgumentCaptor.forClass(Pago.class);
        verify(pagoRepository).save(pago.capture());
        return pago.getValue();
    }

    @Test
    void confirmarCompra_exitosa_dejaElPagoEnElHistorialComoExitosoConSuFolio() throws Exception {
        prepararPendienteEnRedis();
        prepararCuentaBloqueadaConSaldo("100.00");
        GestoPagoProducto producto = productoRecargaDe10Pesos();
        producto.setProducto("Recarga $10");
        producto.setServicio("Telcel");
        when(productoRepository.findByIdProducto(582)).thenReturn(Optional.of(producto));
        when(cipher.encriptar(anyString())).thenReturn("cifrado");
        when(senTxClient.sendTx(any())).thenReturn(respuestaGestoPago("01", "Operacion realizada con exito"));

        CompraResultadoResponse resultado = compraService.confirmarCompra(confirmarConPin("111111"), CLIENTE_ID);

        Pago pago = pagoGuardado();
        assertThat(resultado.getPagoId()).isEqualTo(77);
        assertThat(pago.getEstado()).isEqualTo("EXITOSO");
        assertThat(pago.getOperacionId()).isEqualTo(OPERACION_ID);
        assertThat(pago.getClienteId()).isEqualTo(CLIENTE_ID);
        assertThat(pago.getNombreProducto()).isEqualTo("Recarga $10");
        assertThat(pago.getNombreServicio()).isEqualTo("Telcel");
        assertThat(pago.getTelefono()).isEqualTo("5555555551");
        assertThat(pago.getMonto()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(pago.getIdTransaccion()).isEqualTo("999");
        assertThat(pago.getNumeroAutorizacion()).isEqualTo("AUTH123");
        assertThat(pago.getCodigoRespuesta()).isEqualTo("01");
    }

    @Test
    void confirmarCompra_gestoPagoRechaza_dejaElPagoComoRechazadoConElMotivo() throws Exception {
        prepararPendienteEnRedis();
        prepararCuentaBloqueadaConSaldo("100.00");
        when(cipher.encriptar(anyString())).thenReturn("cifrado");
        when(senTxClient.sendTx(any())).thenReturn(respuestaGestoPago("02", "Saldo insuficiente"));

        CompraConfirmarRequest confirmacion = confirmarConPin("111111");
        assertThatThrownBy(() -> compraService.confirmarCompra(confirmacion, CLIENTE_ID))
                .isInstanceOf(NegocioException.class);

        Pago pago = pagoGuardado();
        assertThat(pago.getEstado()).isEqualTo("RECHAZADO");
        assertThat(pago.getMensaje()).isEqualTo("Saldo insuficiente");
        assertThat(pago.getCodigoRespuesta()).isEqualTo("02");
    }

    @Test
    void confirmarCompra_resultadoNoConfirmado_dejaElPagoComoPendienteSinFolio() throws Exception {
        prepararPendienteEnRedis();
        prepararCuentaBloqueadaConSaldo("100.00");
        when(cipher.encriptar(anyString())).thenReturn("cifrado");
        when(senTxClient.sendTx(any())).thenThrow(RetryableException.class);
        when(senTxClient.confirmTx(any())).thenThrow(RetryableException.class);

        CompraConfirmarRequest confirmacion = confirmarConPin("111111");
        assertThatThrownBy(() -> compraService.confirmarCompra(confirmacion, CLIENTE_ID))
                .isInstanceOf(NegocioException.class);

        Pago pago = pagoGuardado();
        assertThat(pago.getEstado()).isEqualTo("PENDIENTE");
        assertThat(pago.getIdTransaccion()).isNull();
        assertThat(pago.getNumeroAutorizacion()).isNull();
    }

    @Test
    void obtenerHistorial_regresaLosPagosDelClienteConElLogoDelProveedor() {
        Pago pago = new Pago();
        pago.setId(5);
        pago.setIdServicio(133);
        pago.setNombreServicio("Telcel");
        pago.setEstado("EXITOSO");
        pago.setMonto(BigDecimal.TEN);
        pago.setReferencia("REF1");
        pago.setIdTransaccion("999");
        when(pagoRepository.findByClienteIdOrderByIdDesc(CLIENTE_ID)).thenReturn(List.of(pago));

        List<PagoResponse> historial = compraService.obtenerHistorial(CLIENTE_ID);

        assertThat(historial).singleElement().satisfies(p -> {
            assertThat(p.getId()).isEqualTo(5);
            assertThat(p.getNombreServicio()).isEqualTo("Telcel");
            assertThat(p.getEstado()).isEqualTo("EXITOSO");
            assertThat(p.getReferencia()).isEqualTo("REF1");
            assertThat(p.getIdTransaccion()).isEqualTo("999");
            assertThat(p.getLogoUrl()).isEqualTo("https://imagenes.test/servicios/133.png");
        });
    }

    @Test
    void obtenerPago_deOtroClienteONoExistente_lanzaError404() {
        when(pagoRepository.findByIdAndClienteId(5, CLIENTE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compraService.obtenerPago(5, CLIENTE_ID))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void obtenerPago_propio_regresaElDetalle() {
        Pago pago = new Pago();
        pago.setId(5);
        pago.setEstado("PENDIENTE");
        when(pagoRepository.findByIdAndClienteId(5, CLIENTE_ID)).thenReturn(Optional.of(pago));

        assertThat(compraService.obtenerPago(5, CLIENTE_ID).getEstado()).isEqualTo("PENDIENTE");
    }

    // ---------- verificarReferencia ----------

    private GestoPagoProducto servicioConConsulta() {
        GestoPagoProducto servicio = productoRecargaDe10Pesos();
        servicio.setIdProducto(272);
        servicio.setIdServicio(108);
        servicio.setTipoFront(2);
        GestoPagoProducto consulta = new GestoPagoProducto();
        consulta.setIdProducto(398);
        consulta.setIdServicio(108);
        consulta.setTipoFront(4);
        when(productoRepository.findByIdProducto(272)).thenReturn(Optional.of(servicio));
        when(productoRepository.findFirstByIdServicioAndTipoFront(108, 4)).thenReturn(Optional.of(consulta));
        when(cipher.encriptar(anyString())).thenReturn("cifrado");
        return servicio;
    }

    private VerificarReferenciaRequest verificar(Integer idProducto, String referencia) {
        VerificarReferenciaRequest request = new VerificarReferenciaRequest();
        request.setIdProducto(idProducto);
        request.setReferencia(referencia);
        return request;
    }

    private GestoPagoSenTxResponse respuestaVerificacion(String codigo, String texto, String saldo) {
        GestoPagoSenTxResponse respuesta = respuestaGestoPago(codigo, texto);
        respuesta.getMensaje().setSaldo(saldo);
        return respuesta;
    }

    @Test
    void verificarReferencia_valida_regresaElAdeudoYConsultaConElProductoDeConsulta() {
        servicioConConsulta();
        when(senTxClient.verifyReference(any())).thenReturn(respuestaVerificacion("01", "Referencia Valida", "1034.5"));

        ReferenciaVerificadaResponse resultado = compraService.verificarReferencia(verificar(272, "5610440665"));

        assertThat(resultado.getEstado()).isEqualTo("VALIDA");
        assertThat(resultado.getMensaje()).isEqualTo("Referencia Valida");
        assertThat(resultado.getSaldoPendiente()).isEqualByComparingTo("1034.5");
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(cipher).encriptar(payload.capture());
        assertThat(payload.getValue())
                .isEqualTo("{\"idProducto\":398,\"idServicio\":108,\"referencia\":\"5610440665\"}");
    }

    @Test
    void verificarReferencia_validaSinSaldoLegible_regresaSaldoNull() {
        servicioConConsulta();
        when(senTxClient.verifyReference(any())).thenReturn(respuestaVerificacion("01", null, "no-es-numero"));

        ReferenciaVerificadaResponse resultado = compraService.verificarReferencia(verificar(272, "5610440665"));

        assertThat(resultado.getEstado()).isEqualTo("VALIDA");
        assertThat(resultado.getMensaje()).isEqualTo("Referencia valida");
        assertThat(resultado.getSaldoPendiente()).isNull();
    }

    @Test
    void verificarReferencia_rechazadaPorGestoPago_regresaInvalidaConSuMensaje() {
        servicioConConsulta();
        when(senTxClient.verifyReference(any())).thenReturn(respuestaVerificacion("02", "Referencia Invalida", null));

        ReferenciaVerificadaResponse resultado = compraService.verificarReferencia(verificar(272, "000"));

        assertThat(resultado.getEstado()).isEqualTo("INVALIDA");
        assertThat(resultado.getMensaje()).isEqualTo("Referencia Invalida");
        assertThat(resultado.getSaldoPendiente()).isNull();
    }

    @Test
    void verificarReferencia_respuestaSinMensaje_seTrataComoInvalida() {
        servicioConConsulta();
        when(senTxClient.verifyReference(any())).thenReturn(new GestoPagoSenTxResponse());

        ReferenciaVerificadaResponse resultado = compraService.verificarReferencia(verificar(272, "000"));

        assertThat(resultado.getEstado()).isEqualTo("INVALIDA");
        assertThat(resultado.getMensaje()).isEqualTo("GestoPago no reconocio la referencia");
    }

    @Test
    void verificarReferencia_servicioSinConsultaEnLinea_regresaNoVerificableSinLlamarAGestoPago() {
        GestoPagoProducto servicio = productoRecargaDe10Pesos();
        servicio.setTipoFront(2);
        when(productoRepository.findByIdProducto(582)).thenReturn(Optional.of(servicio));
        when(productoRepository.findFirstByIdServicioAndTipoFront(133, 4)).thenReturn(Optional.empty());

        ReferenciaVerificadaResponse resultado = compraService.verificarReferencia(verificar(582, "ABC123"));

        assertThat(resultado.getEstado()).isEqualTo("NO_VERIFICABLE");
        verify(senTxClient, never()).verifyReference(any());
    }

    @Test
    void verificarReferencia_productoQueNoEsPagoDeServicio_lanzaError400() {
        when(productoRepository.findByIdProducto(582)).thenReturn(Optional.of(productoRecargaDe10Pesos()));
        VerificarReferenciaRequest request = verificar(582, "ABC123");

        assertThatThrownBy(() -> compraService.verificarReferencia(request))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    void verificarReferencia_productoNoExiste_lanzaError404() {
        when(productoRepository.findByIdProducto(1)).thenReturn(Optional.empty());
        VerificarReferenciaRequest request = verificar(1, "ABC123");

        assertThatThrownBy(() -> compraService.verificarReferencia(request))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    private CompraRequest requestPagoDeServicio() {
        CompraRequest request = requestRecarga(new BigDecimal("357.40"));
        request.setIdProducto(272);
        request.setTelefono(null);
        request.setReferencia("5610440665");
        return request;
    }

    @Test
    void iniciarCompra_referenciaQueGestoPagoNoValida_lanzaError400YNoMandaPin() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaDe(clienteActivo())));
        reservaDeCuentaDisponible();
        servicioConConsulta();
        when(saldoRepository.findFirstByCuentaIdOrderByIdDesc(CUENTA_ID)).thenReturn(Optional.of(saldoDe("1000.00")));
        when(senTxClient.verifyReference(any())).thenReturn(respuestaVerificacion("02", "Referencia Invalida", null));
        CompraRequest request = requestPagoDeServicio();

        assertThatThrownBy(() -> compraService.iniciarCompra(request, CLIENTE_ID))
                .isInstanceOf(NegocioException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("Referencia Invalida");
        verify(mailService, never()).enviarCodigoVerificacion(any(), any());
        verify(redisTemplate).delete("compra:cuenta-activa:" + CUENTA_ID);
    }

    @Test
    void iniciarCompra_referenciaValidadaPorGestoPago_continuaYMandaPin() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaDe(clienteActivo())));
        reservaDeCuentaDisponible();
        servicioConConsulta();
        when(saldoRepository.findFirstByCuentaIdOrderByIdDesc(CUENTA_ID)).thenReturn(Optional.of(saldoDe("1000.00")));
        when(senTxClient.verifyReference(any())).thenReturn(respuestaVerificacion("01", "Referencia Valida", "357.4"));

        CompraIniciadaResponse resultado = compraService.iniciarCompra(requestPagoDeServicio(), CLIENTE_ID);

        assertThat(resultado.getOperacionId()).isNotBlank();
        verify(mailService).enviarCodigoVerificacion(eq("cliente@correo.com"), anyString());
    }

    @Test
    void iniciarCompra_conValidacionDeReferenciaDesactivada_noConsultaAGestoPago() {
        ReflectionTestUtils.setField(compraService, "exigirValidacionDeReferencia", false);
        GestoPagoProducto servicio = productoRecargaDe10Pesos();
        servicio.setTipoFront(2);
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaDe(clienteActivo())));
        reservaDeCuentaDisponible();
        when(productoRepository.findByIdProducto(272)).thenReturn(Optional.of(servicio));
        when(saldoRepository.findFirstByCuentaIdOrderByIdDesc(CUENTA_ID)).thenReturn(Optional.of(saldoDe("1000.00")));

        compraService.iniciarCompra(requestPagoDeServicio(), CLIENTE_ID);

        verify(productoRepository, never()).findFirstByIdServicioAndTipoFront(any(), any());
        verify(senTxClient, never()).verifyReference(any());
    }
}
