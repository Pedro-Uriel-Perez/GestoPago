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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        verify(mailService, org.mockito.Mockito.atLeastOnce()).enviarCodigoVerificacion(anyString(), pin.capture());
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
        return saldoRepository.findFirstByCuentaIdOrderByFechaMovimientoDesc(cuentaId)
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
            Thread.sleep(300);
            return respuestaGestoPago("01", "Operacion realizada con exito");
        });
        String operacionId = iniciarCompraYObtenerOperacion(cliente);
        String pin = ultimoPinEnviado();

        List<Integer> estados = ejecutarEnParalelo(2,
                () -> confirmar(cliente, operacionId, pin).getResponse().getStatus());

        assertThat(estados).containsExactlyInAnyOrder(200, 404);
        verify(senTxClient, org.mockito.Mockito.times(1)).sendTx(any());
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
}
