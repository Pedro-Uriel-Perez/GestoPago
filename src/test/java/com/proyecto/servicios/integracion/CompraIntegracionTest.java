package com.proyecto.servicios.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoSenTxClient;
import com.proyecto.servicios.entity.clientes.Cuenta;
import com.proyecto.servicios.entity.clientes.Saldo;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.model.gestopago.GestoPagoSenTxResponse;
import com.proyecto.servicios.model.productlist.MensajeExternoDTO;
import com.proyecto.servicios.repositorys.clientes.CuentaRepository;
import com.proyecto.servicios.repositorys.clientes.SaldoRepository;
import com.proyecto.servicios.repositorys.compras.ConciliacionPendienteRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.service.GestoPagoProductListSyncService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.MailService;
import feign.FeignException;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integracion del flujo completo de compra: la aplicacion se
 * levanta entera (Spring Security con el JWT real, controllers, services,
 * repositorios, Flyway, transacciones y bloqueo pesimista reales sobre H2).
 * Solo se sustituyen los sistemas externos que no se pueden (ni se deben)
 * llamar desde una prueba: GestoPago (sendTx/confirmTx y token), Gmail y
 * Redis (este ultimo con un mapa en memoria que se comporta igual para lo
 * que usa CompraServiceImpl).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompraIntegracionTest {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();
    private static final int ID_PRODUCTO_RECARGA = 14302;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private SaldoRepository saldoRepository;

    @Autowired
    private GestoPagoProductoRepository productoRepository;

    @Autowired
    private ConciliacionPendienteRepository conciliacionRepository;

    @MockBean
    private GestoPagoSenTxClient senTxClient;

    @MockBean
    private MailService mailService;

    @MockBean
    private GestoPagoTokenService gestoPagoTokenService;

    @MockBean
    private GestoPagoProductListSyncService productListSyncService;

    @MockBean
    private StringRedisTemplate redisTemplate;

    private final Map<String, String> redisEnMemoria = new ConcurrentHashMap<>();

    /** Llave/IV AES aleatorias por corrida: GestoPago esta simulado y asi ningun valor fijo queda en el repo. */
    @DynamicPropertySource
    static void llaveAesDePrueba(DynamicPropertyRegistry registro) {
        registro.add("gestopago.sendtx.secret-key", () -> aleatorioBase64(32));
        registro.add("gestopago.sendtx.iv", () -> aleatorioBase64(16));
    }

    private static String aleatorioBase64(int bytes) {
        byte[] valor = new byte[bytes];
        new java.security.SecureRandom().nextBytes(valor);
        return java.util.Base64.getEncoder().encodeToString(valor);
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void prepararDoblesYCatalogo() {
        redisEnMemoria.clear();
        ValueOperations<String, String> operaciones = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(operaciones);
        doAnswer(inv -> {
            redisEnMemoria.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(operaciones).set(anyString(), anyString(), any(Duration.class));
        when(operaciones.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenAnswer(inv -> redisEnMemoria.putIfAbsent(inv.getArgument(0), inv.getArgument(1)) == null);
        when(operaciones.get(anyString())).thenAnswer(inv -> redisEnMemoria.get(inv.<String>getArgument(0)));
        when(redisTemplate.hasKey(anyString())).thenAnswer(inv -> redisEnMemoria.containsKey(inv.<String>getArgument(0)));
        when(redisTemplate.delete(anyString())).thenAnswer(inv -> redisEnMemoria.remove(inv.<String>getArgument(0)) != null);

        if (productoRepository.findByIdProducto(ID_PRODUCTO_RECARGA).isEmpty()) {
            GestoPagoProducto recarga = new GestoPagoProducto();
            recarga.setIdProducto(ID_PRODUCTO_RECARGA);
            recarga.setIdServicio(2284);
            recarga.setIdCatTipoServicio(13);
            recarga.setTipoFront(1);
            recarga.setProducto("ABIB 100");
            recarga.setServicio("ABIB");
            recarga.setPrecio("100.0");
            productoRepository.save(recarga);
        }
    }

    // ---------- helpers ----------

    private record ClienteDePrueba(String numeroCuenta, String token, Integer cuentaId) {
    }

    private ClienteDePrueba registrarClienteConSaldo(String saldoInicial) throws Exception {
        int n = SECUENCIA.incrementAndGet();
        char letra = (char) ('A' + (n % 26));
        int digito = n % 10;
        String correo = "integracion" + n + "@correo.com";

        String cuerpo = """
                {
                  "nombre": "Integracion",
                  "apellidoPaterno": "Prueba",
                  "apellidoMaterno": "Compra",
                  "fechaNacimiento": "1999-01-01",
                  "curp": "AEXX990101HDFXYZ%c%d",
                  "rfc": "AEXX990101X%c%d",
                  "sexo": "M",
                  "nacionalidadId": 1,
                  "estadoCivil": "SOLTERO",
                  "correoElectronico": "%s",
                  "password": "contrasena123",
                  "telefonoMovil": "55%08d",
                  "ocupacion": "Ingeniero",
                  "empresa": "Pruebas SA",
                  "ingresoMensual": 15000,
                  "domicilio": {
                    "calle": "Calle Falsa", "numeroExterior": "123", "colonia": "Centro",
                    "municipio": "Ciudad", "estado": "Estado", "codigoPostal": "12345", "pais": "Mexico"
                  }
                }
                """.formatted(letra, digito, letra, digito, correo, n);

        MvcResult registro = mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn();
        String numeroCuenta = leer(registro).path("cuenta").path("numeroCuenta").asText();

        MvcResult login = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correoElectronico\":\"" + correo + "\",\"password\":\"contrasena123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String token = leer(login).path("jwt").asText();

        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta).orElseThrow();
        Saldo deposito = new Saldo();
        deposito.setCuenta(cuenta);
        deposito.setMonto(new BigDecimal(saldoInicial));
        deposito.setTipoMovimiento("DEPOSITO");
        deposito.setDescripcion("Deposito de prueba");
        saldoRepository.save(deposito);

        return new ClienteDePrueba(numeroCuenta, token, cuenta.getId());
    }

    private String cuerpoCompra(String numeroCuenta, String monto) {
        return "{\"numeroCuenta\":\"" + numeroCuenta + "\",\"idProducto\":" + ID_PRODUCTO_RECARGA
                + ",\"monto\":" + monto + ",\"telefono\":\"5555555551\"}";
    }

    private String iniciarCompraYObtenerOperacion(ClienteDePrueba cliente) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/compras")
                        .header("Authorization", "Bearer " + cliente.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCompra(cliente.numeroCuenta(), "100")))
                .andExpect(status().isOk())
                .andReturn();
        return leer(resultado).path("operacionId").asText();
    }

    private String ultimoPinEnviado() {
        ArgumentCaptor<String> pin = ArgumentCaptor.forClass(String.class);
        verify(mailService, atLeastOnce()).enviarCodigoVerificacion(anyString(), pin.capture());
        List<String> pines = pin.getAllValues();
        return pines.get(pines.size() - 1);
    }

    private MvcResult confirmar(ClienteDePrueba cliente, String operacionId, String pin) throws Exception {
        return mockMvc.perform(post("/compras/confirmar")
                        .header("Authorization", "Bearer " + cliente.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operacionId\":\"" + operacionId + "\",\"codigoVerificacion\":\"" + pin + "\"}"))
                .andReturn();
    }

    private GestoPagoSenTxResponse respuestaGestoPago(String codigo, String texto) {
        MensajeExternoDTO mensaje = new MensajeExternoDTO();
        mensaje.setCodigo(codigo);
        mensaje.setTexto(texto);
        GestoPagoSenTxResponse respuesta = new GestoPagoSenTxResponse();
        respuesta.setMensaje(mensaje);
        respuesta.setIdTx("946033922");
        respuesta.setNumAutorizacion("100076595");
        return respuesta;
    }

    private BigDecimal saldoActual(Integer cuentaId) {
        return saldoRepository.findFirstByCuentaIdOrderByIdDesc(cuentaId)
                .map(Saldo::getMonto).orElse(BigDecimal.ZERO);
    }

    private JsonNode leer(MvcResult resultado) throws Exception {
        return objectMapper.readTree(resultado.getResponse().getContentAsString());
    }

    // ---------- pruebas ----------

    @Test
    void flujoCompleto_iniciarYConfirmarConPinCorrecto_descuentaSaldoYRegistraMovimiento() throws Exception {
        ClienteDePrueba cliente = registrarClienteConSaldo("500.00");
        when(senTxClient.sendTx(any())).thenReturn(respuestaGestoPago("01", "Operacion realizada con exito"));

        String operacionId = iniciarCompraYObtenerOperacion(cliente);
        MvcResult resultado = confirmar(cliente, operacionId, ultimoPinEnviado());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(leer(resultado).path("numeroAutorizacion").asText()).isEqualTo("100076595");
        assertThat(leer(resultado).path("saldoRestante").asText()).isEqualTo("400.00");
        assertThat(saldoActual(cliente.cuentaId())).isEqualByComparingTo("400.00");
        assertThat(redisEnMemoria).isEmpty();
    }

    @Test
    void comprar_sinToken_responde401() throws Exception {
        mockMvc.perform(post("/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCompra("1234567890", "100")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void preflightCors_desdeOrigenPermitido_respondeSinPedirToken() throws Exception {
        mockMvc.perform(options("/compras")
                        .header("Origin", "http://localhost:5000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5000"));
    }

    @Test
    void preflightCors_desdeOrigenNoPermitido_seRechaza() throws Exception {
        mockMvc.perform(options("/compras")
                        .header("Origin", "https://sitio-ajeno.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void comprar_conCuentaDeOtroCliente_responde403YNoMandaPin() throws Exception {
        ClienteDePrueba victima = registrarClienteConSaldo("500.00");
        ClienteDePrueba atacante = registrarClienteConSaldo("500.00");

        mockMvc.perform(post("/compras")
                        .header("Authorization", "Bearer " + atacante.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCompra(victima.numeroCuenta(), "100")))
                .andExpect(status().isForbidden());

        verify(mailService, never()).enviarCodigoVerificacion(anyString(), anyString());
    }

    @Test
    void comprar_conMontoManipulado_responde400() throws Exception {
        ClienteDePrueba cliente = registrarClienteConSaldo("500.00");

        mockMvc.perform(post("/compras")
                        .header("Authorization", "Bearer " + cliente.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCompra(cliente.numeroCuenta(), "1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El monto no corresponde al precio de este producto en el catalogo"));
    }

    @Test
    void comprar_sinCamposObligatorios_responde400ConLosCamposInvalidos() throws Exception {
        ClienteDePrueba cliente = registrarClienteConSaldo("500.00");

        MvcResult resultado = mockMvc.perform(post("/compras")
                        .header("Authorization", "Bearer " + cliente.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroCuenta\":\"" + cliente.numeroCuenta() + "\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(leer(resultado).path("mensaje").asText()).contains("idProducto").contains("monto");
    }

    @Test
    void comprar_dosVecesSeguidasSinConfirmar_laSegundaResponde409() throws Exception {
        ClienteDePrueba cliente = registrarClienteConSaldo("500.00");
        iniciarCompraYObtenerOperacion(cliente);

        mockMvc.perform(post("/compras")
                        .header("Authorization", "Bearer " + cliente.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCompra(cliente.numeroCuenta(), "100")))
                .andExpect(status().isConflict());
    }

    @Test
    void confirmar_conPinIncorrecto_responde401YNoLlamaAGestoPago() throws Exception {
        ClienteDePrueba cliente = registrarClienteConSaldo("500.00");
        String operacionId = iniciarCompraYObtenerOperacion(cliente);
        String pinCorrecto = ultimoPinEnviado();
        String pinIncorrecto = pinCorrecto.equals("000000") ? "111111" : "000000";

        MvcResult resultado = confirmar(cliente, operacionId, pinIncorrecto);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        verify(senTxClient, never()).sendTx(any());
    }

    @Test
    void confirmar_timeoutEnSendTxPeroConfirmTxDiceQueSiSeAplico_descuentaSaldo() throws Exception {
        ClienteDePrueba cliente = registrarClienteConSaldo("500.00");
        when(senTxClient.sendTx(any())).thenThrow(RetryableException.class);
        when(senTxClient.confirmTx(any())).thenReturn(respuestaGestoPago("01", "Operacion realizada con exito"));

        String operacionId = iniciarCompraYObtenerOperacion(cliente);
        MvcResult resultado = confirmar(cliente, operacionId, ultimoPinEnviado());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(saldoActual(cliente.cuentaId())).isEqualByComparingTo("400.00");
    }

    @Test
    void confirmar_timeoutEnSendTxYEnConfirmTx_responde202GuardaConciliacionYNoDescuentaSaldo() throws Exception {
        ClienteDePrueba cliente = registrarClienteConSaldo("500.00");
        when(senTxClient.sendTx(any())).thenThrow(RetryableException.class);
        when(senTxClient.confirmTx(any())).thenThrow(RetryableException.class);
        long conciliacionesAntes = conciliacionRepository.count();

        String operacionId = iniciarCompraYObtenerOperacion(cliente);
        MvcResult resultado = confirmar(cliente, operacionId, ultimoPinEnviado());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(202);
        assertThat(conciliacionRepository.count()).isEqualTo(conciliacionesAntes + 1);
        assertThat(saldoActual(cliente.cuentaId())).isEqualByComparingTo("500.00");
        assertThat(redisEnMemoria).containsKey("compra:cuenta-activa:" + cliente.cuentaId());
    }

    @Test
    void confirmar_errorInesperadoDelProveedor_responde502SinExponerDetallesInternos() throws Exception {
        ClienteDePrueba cliente = registrarClienteConSaldo("500.00");
        when(senTxClient.sendTx(any())).thenThrow(FeignException.Unauthorized.class);

        String operacionId = iniciarCompraYObtenerOperacion(cliente);
        MvcResult resultado = confirmar(cliente, operacionId, ultimoPinEnviado());

        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(502);
        assertThat(cuerpo).doesNotContain("Exception").doesNotContain("gestopago.portalventas.net").doesNotContain("at com.");
        assertThat(saldoActual(cliente.cuentaId())).isEqualByComparingTo("500.00");
    }

    @Test
    void concurrencia_dosComprasSimultaneasDeLaMismaCuenta_soloUnaSeInicia() throws Exception {
        ClienteDePrueba cliente = registrarClienteConSaldo("500.00");

        List<Integer> estados = ejecutarEnParalelo(2, () -> mockMvc.perform(post("/compras")
                        .header("Authorization", "Bearer " + cliente.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCompra(cliente.numeroCuenta(), "100")))
                .andReturn().getResponse().getStatus());

        assertThat(estados).containsExactlyInAnyOrder(200, 409);
    }

    @Test
    void concurrencia_dobleConfirmacionSimultaneaDeLaMismaOperacion_cobraUnaSolaVez() throws Exception {
        ClienteDePrueba cliente = registrarClienteConSaldo("500.00");
        when(senTxClient.sendTx(any())).thenAnswer(inv -> {
            // Simula la latencia de GestoPago para que las dos confirmaciones
            // se traslapen de verdad. Lo que se verifica (un solo cobro) no
            // depende de cuanto dure la espera, solo hace la prueba mas exigente.
            Thread.sleep(300); // NOSONAR java:S2925, latencia simulada del proveedor externo
            return respuestaGestoPago("01", "Operacion realizada con exito");
        });
        String operacionId = iniciarCompraYObtenerOperacion(cliente);
        String pin = ultimoPinEnviado();

        List<Integer> estados = ejecutarEnParalelo(2,
                () -> confirmar(cliente, operacionId, pin).getResponse().getStatus());

        assertThat(estados).containsExactlyInAnyOrder(200, 404);
        verify(senTxClient, times(1)).sendTx(any());
        assertThat(saldoActual(cliente.cuentaId())).isEqualByComparingTo("400.00");
    }

    private List<Integer> ejecutarEnParalelo(int hilos, java.util.concurrent.Callable<Integer> tarea) throws Exception {
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(hilos);
        java.util.concurrent.CountDownLatch salida = new java.util.concurrent.CountDownLatch(1);
        try {
            List<java.util.concurrent.Future<Integer>> futuros = new java.util.ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                futuros.add(pool.submit(() -> {
                    salida.await();
                    return tarea.call();
                }));
            }
            salida.countDown();
            List<Integer> estados = new java.util.ArrayList<>();
            for (java.util.concurrent.Future<Integer> futuro : futuros) {
                estados.add(futuro.get(30, java.util.concurrent.TimeUnit.SECONDS));
            }
            return estados;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void confirmar_gestoPagoRechazaPorDuplicado_responde409ConElMensajeDelProveedor() throws Exception {
        ClienteDePrueba cliente = registrarClienteConSaldo("500.00");
        when(senTxClient.sendTx(any())).thenReturn(respuestaGestoPago("06",
                "Transaccion aplicada anteriormente. Intentar dentro de 15 minutos."));

        String operacionId = iniciarCompraYObtenerOperacion(cliente);
        MvcResult resultado = confirmar(cliente, operacionId, ultimoPinEnviado());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(leer(resultado).path("mensaje").asText()).contains("aplicada anteriormente");
        assertThat(saldoActual(cliente.cuentaId())).isEqualByComparingTo("500.00");
        verify(senTxClient, never()).confirmTx(any());
    }

    // ---------- historial, sesiones por dispositivo, deposito y referencias ----------

    private String iniciarSesion(String correo, String dispositivo) throws Exception {
        MvcResult login = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correoElectronico\":\"" + correo + "\",\"password\":\"contrasena123\","
                                + "\"dispositivo\":\"" + dispositivo + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return leer(login).path("jwt").asText();
    }

    private String correoDelUltimoCliente() {
        return "integracion" + SECUENCIA.get() + "@correo.com";
    }

    @Test
    void historial_despuesDeUnPagoExitoso_loMuestraConFolioYSoloAlDueno() throws Exception {
        ClienteDePrueba cliente = registrarClienteConSaldo("500.00");
        when(senTxClient.sendTx(any())).thenReturn(respuestaGestoPago("01", "Operacion realizada con exito"));
        String operacionId = iniciarCompraYObtenerOperacion(cliente);
        MvcResult pago = confirmar(cliente, operacionId, ultimoPinEnviado());
        int pagoId = leer(pago).path("pagoId").asInt();

        mockMvc.perform(get("/compras").header("Authorization", "Bearer " + cliente.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(pagoId))
                .andExpect(jsonPath("$[0].estado").value("EXITOSO"))
                .andExpect(jsonPath("$[0].nombreServicio").value("ABIB"))
                .andExpect(jsonPath("$[0].idTransaccion").value("946033922"))
                .andExpect(jsonPath("$[0].telefono").value("5555555551"))
                .andExpect(jsonPath("$[0].monto").value(100))
                .andExpect(jsonPath("$[0].logoUrl").value(org.hamcrest.Matchers.endsWith("/servicios/2284.png")));

        mockMvc.perform(get("/compras/" + pagoId).header("Authorization", "Bearer " + cliente.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operacionId").value(operacionId));

        ClienteDePrueba otro = registrarClienteConSaldo("10.00");
        mockMvc.perform(get("/compras/" + pagoId).header("Authorization", "Bearer " + otro.token()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/compras").header("Authorization", "Bearer " + otro.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void historial_pagoRechazadoYPagoSinConfirmar_quedanConSuEstado() throws Exception {
        ClienteDePrueba cliente = registrarClienteConSaldo("500.00");
        when(senTxClient.sendTx(any())).thenReturn(respuestaGestoPago("02", "Saldo insuficiente"));
        confirmar(cliente, iniciarCompraYObtenerOperacion(cliente), ultimoPinEnviado());

        when(senTxClient.sendTx(any())).thenThrow(RetryableException.class);
        when(senTxClient.confirmTx(any())).thenThrow(RetryableException.class);
        ClienteDePrueba otro = registrarClienteConSaldo("500.00");
        confirmar(otro, iniciarCompraYObtenerOperacion(otro), ultimoPinEnviado());

        mockMvc.perform(get("/compras").header("Authorization", "Bearer " + cliente.token()))
                .andExpect(jsonPath("$[0].estado").value("RECHAZADO"))
                .andExpect(jsonPath("$[0].mensaje").value("Saldo insuficiente"));
        mockMvc.perform(get("/compras").header("Authorization", "Bearer " + otro.token()))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"));
    }

    @Test
    void sesiones_webYMovilFuncionanALaVezYSeAvisaDelNuevoInicio() throws Exception {
        ClienteDePrueba cliente = registrarClienteConSaldo("10.00");
        String tokenWeb = cliente.token();
        String tokenMovil = iniciarSesion(correoDelUltimoCliente(), "MOVIL");

        verify(mailService).enviarAlertaInicioSesion(eq(correoDelUltimoCliente()), eq("MOVIL"), any());
        mockMvc.perform(get("/compras").header("Authorization", "Bearer " + tokenWeb)).andExpect(status().isOk());
        mockMvc.perform(get("/compras").header("Authorization", "Bearer " + tokenMovil)).andExpect(status().isOk());

        mockMvc.perform(get("/login/sesiones").header("Authorization", "Bearer " + tokenWeb))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.dispositivo == 'WEB')].actual").value(true))
                .andExpect(jsonPath("$[?(@.dispositivo == 'MOVIL')].actual").value(false));
    }

    @Test
    void sesiones_cerrarLaMovilNoCierraLaWeb() throws Exception {
        ClienteDePrueba cliente = registrarClienteConSaldo("10.00");
        String tokenMovil = iniciarSesion(correoDelUltimoCliente(), "MOVIL");
        Integer clienteId = cuentaRepository.findById(cliente.cuentaId()).orElseThrow().getCliente().getId();

        mockMvc.perform(post("/login/" + clienteId + "/cerrar").header("Authorization", "Bearer " + tokenMovil))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/compras").header("Authorization", "Bearer " + tokenMovil))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/compras").header("Authorization", "Bearer " + cliente.token()))
                .andExpect(status().isOk());
    }

    @Test
    void sesiones_volverAEntrarEnElMismoDispositivoInvalidaElTokenAnterior() throws Exception {
        ClienteDePrueba cliente = registrarClienteConSaldo("10.00");
        String tokenWebNuevo = iniciarSesion(correoDelUltimoCliente(), "WEB");

        mockMvc.perform(get("/compras").header("Authorization", "Bearer " + cliente.token()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/compras").header("Authorization", "Bearer " + tokenWebNuevo))
                .andExpect(status().isOk());
    }

    @Test
    void login_conDispositivoDesconocido_responde400() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correoElectronico\":\"a@correo.com\",\"password\":\"x\",\"dispositivo\":\"TABLET\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deposito_sumaAlSaldoDeLaCuentaPropiaYRechazaCuentasAjenas() throws Exception {
        ClienteDePrueba cliente = registrarClienteConSaldo("100.00");
        ClienteDePrueba otro = registrarClienteConSaldo("100.00");
        String cuerpo = "{\"numeroCuenta\":\"" + cliente.numeroCuenta() + "\",\"monto\":250.50}";

        mockMvc.perform(post("/cuentas/depositos").header("Authorization", "Bearer " + cliente.token())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoActual").value(350.50));
        assertThat(saldoActual(cliente.cuentaId())).isEqualByComparingTo("350.50");

        mockMvc.perform(post("/cuentas/depositos").header("Authorization", "Bearer " + otro.token())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/cuentas/depositos").header("Authorization", "Bearer " + cliente.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroCuenta\":\"" + cliente.numeroCuenta() + "\",\"monto\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verificarReferencia_respondeSegunLoQueDiceGestoPago() throws Exception {
        ClienteDePrueba cliente = registrarClienteConSaldo("10.00");
        if (productoRepository.findByIdProducto(272).isEmpty()) {
            GestoPagoProducto pago = new GestoPagoProducto();
            pago.setIdProducto(272);
            pago.setIdServicio(108);
            pago.setTipoFront(2);
            pago.setProducto("Pago de Recibo AT-T");
            pago.setServicio("AT-T");
            productoRepository.save(pago);
            GestoPagoProducto consulta = new GestoPagoProducto();
            consulta.setIdProducto(398);
            consulta.setIdServicio(108);
            consulta.setTipoFront(4);
            consulta.setProducto("Consulta de Saldo");
            consulta.setServicio("AT-T");
            productoRepository.save(consulta);
        }
        MensajeExternoDTO valida = new MensajeExternoDTO();
        valida.setCodigo("01");
        valida.setTexto("Referencia Valida");
        valida.setSaldo("1034.5");
        GestoPagoSenTxResponse respuestaValida = new GestoPagoSenTxResponse();
        respuestaValida.setMensaje(valida);
        when(senTxClient.verifyReference(any())).thenReturn(respuestaValida);

        mockMvc.perform(post("/compras/verificar-referencia").header("Authorization", "Bearer " + cliente.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idProducto\":272,\"referencia\":\"5610440665\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VALIDA"))
                .andExpect(jsonPath("$.saldoPendiente").value(1034.5));

        when(senTxClient.verifyReference(any())).thenReturn(respuestaGestoPago("02", "Referencia Invalida"));
        mockMvc.perform(post("/compras/verificar-referencia").header("Authorization", "Bearer " + cliente.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idProducto\":272,\"referencia\":\"000000000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INVALIDA"))
                .andExpect(jsonPath("$.mensaje").value("Referencia Invalida"));

        mockMvc.perform(post("/compras/verificar-referencia").header("Authorization", "Bearer " + cliente.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idProducto\":272,\"referencia\":\"con espacios!\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/compras/verificar-referencia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idProducto\":272,\"referencia\":\"5610440665\"}"))
                .andExpect(status().isUnauthorized());
    }
}
