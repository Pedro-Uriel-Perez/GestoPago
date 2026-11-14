package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.ZonaHoraria;
import org.springframework.http.HttpStatus;
import com.proyecto.servicios.exception.NegocioException;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.proyecto.servicios.model.gestopago.GestoPagoSenTxRequest;
import com.proyecto.servicios.model.gestopago.GestoPagoSenTxResponse;
import com.proyecto.servicios.repositorys.clientes.CuentaRepository;
import com.proyecto.servicios.repositorys.clientes.SaldoRepository;
import com.proyecto.servicios.repositorys.compras.ConciliacionPendienteRepository;
import com.proyecto.servicios.repositorys.compras.PagoRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.security.GestoPagoAesCipher;
import com.proyecto.servicios.service.CompraService;
import com.proyecto.servicios.service.MailService;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class CompraServiceImpl implements CompraService {

    private static final Duration TTL_OPERACION = Duration.ofMinutes(5);
    private static final DateTimeFormatter FORMATO_HORA_LOCAL = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String PREFIJO_CLAVE_PENDIENTE = "compra:pendiente:";
    private static final String PREFIJO_CLAVE_ACTIVA = "compra:cuenta-activa:";
    private static final int TIPO_FRONT_PAGO_SERVICIO = 2;
    private static final int TIPO_FRONT_CONSULTA = 4;

    private final CuentaRepository cuentaRepository;
    private final SaldoRepository saldoRepository;
    private final GestoPagoProductoRepository productoRepository;
    private final ConciliacionPendienteRepository conciliacionRepository;
    private final PagoRepository pagoRepository;
    private final GestoPagoCatalogo catalogo;
    private final EvidenciaGestoPago bitacora;
    private final GestoPagoSenTxClient senTxClient;
    private final GestoPagoAesCipher cipher;
    private final MailService mailService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SecureRandom generadorAleatorio = new SecureRandom();

    @Value("${gestopago.sendtx.unidad}")
    private String unidad;

    @Value("${gestopago.referencias.exigir-validacion:true}")
    private boolean exigirValidacionDeReferencia;

    public CompraServiceImpl(CuentaRepository cuentaRepository,
                              SaldoRepository saldoRepository,
                              GestoPagoProductoRepository productoRepository,
                              ConciliacionPendienteRepository conciliacionRepository,
                              PagoRepository pagoRepository,
                              GestoPagoCatalogo catalogo,
                              EvidenciaGestoPago bitacora,
                              GestoPagoSenTxClient senTxClient,
                              GestoPagoAesCipher cipher,
                              MailService mailService,
                              StringRedisTemplate redisTemplate,
                              ObjectMapper objectMapper) {
        this.cuentaRepository = cuentaRepository;
        this.saldoRepository = saldoRepository;
        this.productoRepository = productoRepository;
        this.conciliacionRepository = conciliacionRepository;
        this.pagoRepository = pagoRepository;
        this.catalogo = catalogo;
        this.bitacora = bitacora;
        this.senTxClient = senTxClient;
        this.cipher = cipher;
        this.mailService = mailService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public CompraIniciadaResponse iniciarCompra(CompraRequest request, Integer clienteIdAutenticado) {
        Cuenta cuenta = buscarCuenta(request.getNumeroCuenta());
        Cliente cliente = cuenta.getCliente();

        if (!cliente.getId().equals(clienteIdAutenticado)) {
            throw new NegocioException(HttpStatus.FORBIDDEN,
                    "La cuenta indicada no pertenece al cliente autenticado");
        }
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new NegocioException(HttpStatus.FORBIDDEN, "El cliente no esta activo, no puede realizar compras");
        }

        String operacionId = UUID.randomUUID().toString();
        reservarCuentaParaOperacion(cuenta.getId(), operacionId);
        try {
            GestoPagoProducto producto = productoRepository.findByIdProducto(request.getIdProducto())
                    .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND,
                            "No existe un producto GestoPago con idProducto " + request.getIdProducto()));

            validarMontoCorrespondeAlCatalogo(producto, request.getMonto());

            BigDecimal saldoActual = saldoVigente(cuenta.getId());
            if (saldoActual.compareTo(request.getMonto()) < 0) {
                throw new NegocioException(HttpStatus.CONFLICT, "El saldo de la cuenta es insuficiente para esta compra");
            }

            String telefono = resolverTelefono(producto, request);
            String referencia = resolverReferencia(producto, request);
            exigirReferenciaValidada(producto, referencia);
            String codigoVerificacion = generarCodigoVerificacion();

            CompraPendiente pendiente = new CompraPendiente(
                    cliente.getId(),
                    cuenta.getId(),
                    producto.getIdProducto(),
                    producto.getIdServicio(),
                    telefono,
                    referencia,
                    referencia != null ? request.getMonto().doubleValue() : null,
                    generarUpc(),
                    unidad,
                    LocalDateTime.now(ZonaHoraria.MEXICO).format(FORMATO_HORA_LOCAL),
                    request.getMonto(),
                    codigoVerificacion);

            guardarPendiente(operacionId, pendiente);
            mailService.enviarCodigoVerificacion(cliente.getCorreoElectronico(), codigoVerificacion);
        } catch (RuntimeException e) {
            liberarOperacion(operacionId, cuenta.getId());
            throw e;
        }

        log.info("Compra iniciada, pendiente de verificacion 2FA. operacionId={}, clienteId={}",
                operacionId, cliente.getId());
        return new CompraIniciadaResponse(operacionId, "Se envio un codigo de verificacion a tu correo electronico");
    }

    /**
     * SETNX atomico en Redis: si dos peticiones de la misma cuenta llegan al
     * mismo tiempo, solo una obtiene la reserva. Revisar primero si existe
     * la clave y despues crearla (dos pasos) dejaria pasar a ambas.
     */
    private void reservarCuentaParaOperacion(Integer cuentaId, String operacionId) {
        Boolean reservada = redisTemplate.opsForValue().setIfAbsent(claveActiva(cuentaId), operacionId, TTL_OPERACION);
        if (!Boolean.TRUE.equals(reservada)) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    "Ya existe una compra pendiente de confirmar para esta cuenta, espera a que expire o completala");
        }
    }

    /**
     * @Transactional a proposito, aunque la regla general del proyecto es
     * evitarlo fuera de escrituras multi-tabla: aqui se necesita una
     * transaccion real para sostener el bloqueo pesimista de
     * findWithLockingById mientras se verifica el saldo, se llama a
     * GestoPago y se registra el descuento como una sola seccion
     * critica. Sin esto, dos confirmaciones simultaneas de la misma cuenta
     * podrian leer el mismo saldo "antes" y ambas restar sobre el mismo
     * valor (lost update).
     *
     * Nota de diseno: el candado se mantiene abierto durante la llamada
     * externa a GestoPago (hasta 62s). Eso bloquea otras operaciones SOLO
     * sobre esta misma cuenta (no afecta a otras cuentas en paralelo), que
     * es el trade-off aceptado aqui a cambio de una implementacion mucho
     * mas simple y facil de verificar que un esquema de reserva en dos
     * fases.
     *
     * noRollbackFor: cuando el resultado queda indeterminado, el registro de
     * conciliacion (ConciliacionPendiente) se guarda justo antes de lanzar
     * la NegocioException (202); sin esto el rollback automatico lo
     * borraria. Es seguro aplicarlo a toda NegocioException de este metodo
     * porque ninguna se lanza despues de escribir otra cosa: el descuento
     * de saldo es lo ultimo que ocurre y solo en el camino exitoso. Errores
     * inesperados (de base de datos, etc.) siguen haciendo rollback.
     */
    @Override
    @Transactional(noRollbackFor = NegocioException.class)
    public CompraResultadoResponse confirmarCompra(CompraConfirmarRequest request, Integer clienteIdAutenticado) {
        CompraPendiente pendiente = leerPendiente(request.getOperacionId())
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND,
                        "La operacion no existe o ya expiro, inicia la compra nuevamente"));

        if (!pendiente.getClienteId().equals(clienteIdAutenticado)) {
            throw new NegocioException(HttpStatus.FORBIDDEN, "Esta operacion no pertenece al cliente autenticado");
        }
        if (!pendiente.getCodigoVerificacion().equals(request.getCodigoVerificacion())) {
            throw new NegocioException(HttpStatus.UNAUTHORIZED, "El codigo de verificacion no es correcto");
        }

        Cuenta cuenta = cuentaRepository.findWithLockingById(pendiente.getCuentaId())
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "La cuenta de la compra ya no existe"));

        // Doble envio (ej. doble clic): la segunda confirmacion de la misma
        // operacion se queda esperando el candado de la primera; cuando lo
        // obtiene, la primera ya la proceso y borro de Redis, asi que aqui
        // se corta sin volver a llamar a GestoPago.
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(clavePendiente(request.getOperacionId())))) {
            throw new NegocioException(HttpStatus.NOT_FOUND, "La operacion ya fue procesada o expiro");
        }

        BigDecimal saldoActual = saldoVigente(cuenta.getId());
        if (saldoActual.compareTo(pendiente.getMonto()) < 0) {
            liberarOperacion(request.getOperacionId(), pendiente.getCuentaId());
            throw new NegocioException(HttpStatus.CONFLICT, "El saldo de la cuenta es insuficiente para esta compra");
        }

        GestoPagoSenTxResponse respuesta = ejecutarSendTxConConciliacion(request.getOperacionId(), pendiente);

        String codigo = respuesta.getMensaje() != null ? respuesta.getMensaje().getCodigo() : null;
        log.info("Respuesta GestoPago. operacionId={}, clienteId={}, upc={}, idProducto={}, codigo={}, "
                        + "idTx={}, numAutorizacion={}",
                request.getOperacionId(), pendiente.getClienteId(), pendiente.getUpc(),
                pendiente.getIdProducto(), codigo, respuesta.getIdTx(), respuesta.getNumAutorizacion());

        if (!respuesta.esExitoso()) {
            liberarOperacion(request.getOperacionId(), pendiente.getCuentaId());
            String mensaje = respuesta.getMensaje() != null && respuesta.getMensaje().getTexto() != null
                    ? respuesta.getMensaje().getTexto()
                    : "GestoPago rechazo la transaccion (codigo " + codigo + ")";
            registrarPago(request.getOperacionId(), pendiente, Pago.ESTADO_RECHAZADO, respuesta, mensaje);
            throw new NegocioException(HttpStatus.CONFLICT, mensaje);
        }

        BigDecimal saldoRestante = registrarMovimientoSaldo(cuenta, saldoActual, pendiente, respuesta);
        Pago pago = registrarPago(request.getOperacionId(), pendiente, Pago.ESTADO_EXITOSO, respuesta,
                "Transaccion exitosa");
        liberarOperacion(request.getOperacionId(), pendiente.getCuentaId());

        CompraResultadoResponse resultado = new CompraResultadoResponse();
        resultado.setNumeroAutorizacion(respuesta.getNumAutorizacion());
        resultado.setIdTransaccion(respuesta.getIdTx());
        resultado.setSaldoRestante(saldoRestante.toPlainString());
        resultado.setMensaje("Transaccion exitosa");
        resultado.setPagoId(pago.getId());
        return resultado;
    }

    /**
     * Llama sendTx; si la falla es de las que NO permiten saber si GestoPago
     * alcanzo a procesar la solicitud (timeout, error de servidor), NO se
     * asume que fallo: se consulta confirmTx con los mismos parametros
     * exactos, tal como exige la especificacion. Un error claro y sincrono
     * (ej. credenciales invalidas, 4xx) si se puede asumir que no se
     * proceso, asi que esos se dejan propagar tal cual.
     */
    private GestoPagoSenTxResponse ejecutarSendTxConConciliacion(String operacionId, CompraPendiente pendiente) {
        MultiValueMap<String, String> form = construirFormularioCifrado(pendiente);
        try {
            return bitacora.registrar("sendTx", pendiente.getUpc(), payloadEnClaro(pendiente),
                    () -> senTxClient.sendTx(form));
        } catch (RetryableException e) {
            log.error("sendTx no respondio a tiempo, no se puede asumir que la transaccion no se proceso. "
                    + "Consultando confirmTx. upc={}", pendiente.getUpc(), e);
            return consultarConfirmTx(operacionId, pendiente, form);
        } catch (FeignException.InternalServerError | FeignException.ServiceUnavailable
                | FeignException.BadGateway | FeignException.GatewayTimeout e) {
            log.error("GestoPago respondio con error de servidor en sendTx, consultando confirmTx para conciliar. "
                    + "upc={}", pendiente.getUpc(), e);
            return consultarConfirmTx(operacionId, pendiente, form);
        }
    }

    private GestoPagoSenTxResponse consultarConfirmTx(String operacionId, CompraPendiente pendiente,
                                                      MultiValueMap<String, String> form) {
        try {
            return bitacora.registrar("confirmTx", pendiente.getUpc(), payloadEnClaro(pendiente),
                    () -> senTxClient.confirmTx(form));
        } catch (Exception e) {
            log.error("confirmTx tampoco permitio determinar el estado real de la transaccion. "
                    + "Se registra para conciliacion manual, no se reintenta solo. upc={}, idProducto={}",
                    pendiente.getUpc(), pendiente.getIdProducto(), e);
            registrarConciliacionPendiente(pendiente);
            String mensaje = "No fue posible confirmar si la transaccion se proceso en GestoPago. "
                    + "Se va a verificar manualmente, no la reintentes.";
            registrarPago(operacionId, pendiente, Pago.ESTADO_PENDIENTE, null, mensaje);
            throw new NegocioException(HttpStatus.ACCEPTED, mensaje);
        }
    }

    private void registrarConciliacionPendiente(CompraPendiente pendiente) {
        ConciliacionPendiente conciliacion = new ConciliacionPendiente();
        conciliacion.setCuentaId(pendiente.getCuentaId());
        conciliacion.setClienteId(pendiente.getClienteId());
        conciliacion.setUpc(pendiente.getUpc());
        conciliacion.setIdProducto(pendiente.getIdProducto());
        conciliacion.setMonto(pendiente.getMonto());
        conciliacionRepository.save(conciliacion);
    }

    /**
     * Deja el pago en el historial del cliente con su resultado. El nombre
     * del producto/servicio se copia del catalogo para que el historial no
     * cambie si el catalogo cambia despues.
     */
    private Pago registrarPago(String operacionId, CompraPendiente pendiente, String estado,
                               GestoPagoSenTxResponse respuesta, String mensaje) {
        Pago pago = new Pago();
        pago.setClienteId(pendiente.getClienteId());
        pago.setCuentaId(pendiente.getCuentaId());
        pago.setOperacionId(operacionId);
        pago.setIdProducto(pendiente.getIdProducto());
        pago.setIdServicio(pendiente.getIdServicio());
        productoRepository.findByIdProducto(pendiente.getIdProducto()).ifPresent(producto -> {
            pago.setNombreProducto(producto.getProducto());
            pago.setNombreServicio(producto.getServicio());
        });
        // En pagos de servicio el telefono es un relleno fijo que pide
        // GestoPago, no un dato del cliente: no se guarda en el historial.
        pago.setTelefono(pendiente.getReferencia() == null ? pendiente.getTelefono() : null);
        pago.setReferencia(pendiente.getReferencia());
        pago.setMonto(pendiente.getMonto());
        pago.setEstado(estado);
        pago.setMensaje(mensaje);
        Optional.ofNullable(respuesta).ifPresent(r -> {
            pago.setIdTransaccion(r.getIdTx());
            pago.setNumeroAutorizacion(r.getNumAutorizacion());
            pago.setCodigoRespuesta(r.getMensaje() != null ? r.getMensaje().getCodigo() : null);
        });
        return pagoRepository.save(pago);
    }

    @Override
    public ReferenciaVerificadaResponse verificarReferencia(VerificarReferenciaRequest request) {
        GestoPagoProducto producto = productoRepository.findByIdProducto(request.getIdProducto())
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND,
                        "No existe un producto GestoPago con idProducto " + request.getIdProducto()));
        if (!esPagoDeServicio(producto)) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Este producto no se paga con referencia");
        }
        return consultarReferencia(producto, request.getReferencia());
    }

    /**
     * verifyReference.do solo existe para los productos tipoFront 4
     * ("consulta de saldo"): se busca el del mismo servicio que el producto
     * que se quiere pagar y la consulta se hace con ese idProducto.
     */
    private ReferenciaVerificadaResponse consultarReferencia(GestoPagoProducto producto, String referencia) {
        Optional<GestoPagoProducto> consulta = productoRepository
                .findFirstByIdServicioAndTipoFront(producto.getIdServicio(), TIPO_FRONT_CONSULTA);
        if (consulta.isEmpty()) {
            return new ReferenciaVerificadaResponse(ReferenciaVerificadaResponse.NO_VERIFICABLE,
                    "GestoPago no ofrece validacion en linea para este servicio; revisa que la referencia sea correcta",
                    null);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("idProducto", consulta.get().getIdProducto());
        payload.put("idServicio", producto.getIdServicio());
        payload.put("referencia", referencia);

        String payloadJson = escribirJson(payload);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("random", String.valueOf(System.currentTimeMillis()));
        form.add("signed", cipher.encriptar(payloadJson));

        GestoPagoSenTxResponse respuesta = bitacora.registrar("verifyReference", null, payloadJson,
                () -> senTxClient.verifyReference(form));
        String texto = respuesta.getMensaje() != null ? respuesta.getMensaje().getTexto() : null;
        log.info("Referencia verificada con GestoPago. idProducto={}, idServicio={}, codigo={}",
                producto.getIdProducto(), producto.getIdServicio(),
                respuesta.getMensaje() != null ? respuesta.getMensaje().getCodigo() : null);

        if (!respuesta.esExitoso()) {
            return new ReferenciaVerificadaResponse(ReferenciaVerificadaResponse.INVALIDA,
                    Optional.ofNullable(texto).orElse("GestoPago no reconocio la referencia"), null);
        }
        return new ReferenciaVerificadaResponse(ReferenciaVerificadaResponse.VALIDA,
                Optional.ofNullable(texto).orElse("Referencia valida"),
                leerSaldo(respuesta.getMensaje().getSaldo()));
    }

    private BigDecimal leerSaldo(String saldo) {
        try {
            return saldo == null ? null : new BigDecimal(saldo.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * No se deja iniciar el pago de un servicio si GestoPago dice que la
     * referencia no es valida. Los servicios sin consulta en linea
     * (NO_VERIFICABLE) si pasan: ahi no hay contra que validar.
     */
    private void exigirReferenciaValidada(GestoPagoProducto producto, String referencia) {
        if (referencia == null || !exigirValidacionDeReferencia) {
            return;
        }
        ReferenciaVerificadaResponse verificacion = consultarReferencia(producto, referencia);
        if (ReferenciaVerificadaResponse.INVALIDA.equals(verificacion.getEstado())) {
            throw new NegocioException(HttpStatus.BAD_REQUEST,
                    "La referencia no fue validada por GestoPago: " + verificacion.getMensaje());
        }
    }

    @Override
    public List<PagoResponse> obtenerHistorial(Integer clienteIdAutenticado) {
        return pagoRepository.findByClienteIdOrderByIdDesc(clienteIdAutenticado).stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Override
    public PagoResponse obtenerPago(Integer pagoId, Integer clienteIdAutenticado) {
        // Mismo 404 si el pago no existe o si es de otro cliente, para no
        // revelar que ids de pago existen.
        return pagoRepository.findByIdAndClienteId(pagoId, clienteIdAutenticado)
                .map(this::aRespuesta)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "No existe un pago con id " + pagoId));
    }

    private PagoResponse aRespuesta(Pago pago) {
        PagoResponse respuesta = new PagoResponse();
        respuesta.setId(pago.getId());
        respuesta.setOperacionId(pago.getOperacionId());
        respuesta.setIdProducto(pago.getIdProducto());
        respuesta.setIdServicio(pago.getIdServicio());
        respuesta.setNombreProducto(pago.getNombreProducto());
        respuesta.setNombreServicio(pago.getNombreServicio());
        respuesta.setLogoUrl(catalogo.logoUrl(pago.getIdServicio()));
        respuesta.setTelefono(pago.getTelefono());
        respuesta.setReferencia(pago.getReferencia());
        respuesta.setMonto(pago.getMonto());
        respuesta.setEstado(pago.getEstado());
        respuesta.setIdTransaccion(pago.getIdTransaccion());
        respuesta.setNumeroAutorizacion(pago.getNumeroAutorizacion());
        respuesta.setMensaje(pago.getMensaje());
        respuesta.setFecha(pago.getFecha());
        return respuesta;
    }

    private boolean esPagoDeServicio(GestoPagoProducto producto) {
        return Integer.valueOf(TIPO_FRONT_PAGO_SERVICIO).equals(producto.getTipoFront());
    }

    /** El mismo JSON que se cifra y se manda a GestoPago, para la bitacora. */
    private String payloadEnClaro(CompraPendiente pendiente) {
        return escribirJson(payloadDe(pendiente));
    }

    private GestoPagoSenTxRequest payloadDe(CompraPendiente pendiente) {
        return GestoPagoSenTxRequest.builder()
                .idProducto(pendiente.getIdProducto())
                .idServicio(pendiente.getIdServicio())
                .telefono(pendiente.getTelefono())
                .horaLocal(pendiente.getHoraLocal())
                .referencia(pendiente.getReferencia())
                .montoPago(pendiente.getMontoPago())
                .upc(pendiente.getUpc())
                .unidad(pendiente.getUnidad())
                .build();
    }

    private MultiValueMap<String, String> construirFormularioCifrado(CompraPendiente pendiente) {
        String signed = cipher.encriptar(payloadEnClaro(pendiente));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("random", String.valueOf(System.currentTimeMillis()));
        form.add("signed", signed);
        return form;
    }

    private BigDecimal registrarMovimientoSaldo(Cuenta cuenta, BigDecimal saldoActual,
                                                 CompraPendiente pendiente, GestoPagoSenTxResponse respuesta) {
        BigDecimal saldoNuevo = saldoActual.subtract(pendiente.getMonto());

        Saldo movimiento = new Saldo();
        movimiento.setCuenta(cuenta);
        movimiento.setMonto(saldoNuevo);
        movimiento.setTipoMovimiento("COMPRA");
        movimiento.setDescripcion("Compra GestoPago, autorizacion " + respuesta.getNumAutorizacion());
        saldoRepository.save(movimiento);

        return saldoNuevo;
    }

    private void validarMontoCorrespondeAlCatalogo(GestoPagoProducto producto, BigDecimal montoSolicitado) {
        boolean esPagoDeServicio = esPagoDeServicio(producto);
        if (esPagoDeServicio || producto.getPrecio() == null) {
            // El monto de un pago de servicio lo define el recibo/convenio,
            // no el catalogo; y si el catalogo no trae precio no hay nada
            // contra que validar.
            return;
        }
        BigDecimal precioCatalogo;
        try {
            precioCatalogo = new BigDecimal(producto.getPrecio());
        } catch (NumberFormatException e) {
            return;
        }
        if (precioCatalogo.compareTo(montoSolicitado) != 0) {
            throw new NegocioException(HttpStatus.BAD_REQUEST,
                    "El monto no corresponde al precio de este producto en el catalogo");
        }
    }

    private String resolverTelefono(GestoPagoProducto producto, CompraRequest request) {
        Integer idCatTipoServicio = producto.getIdCatTipoServicio();
        if (idCatTipoServicio != null && (idCatTipoServicio == 10 || idCatTipoServicio == 11)) {
            return "#1111111111";
        }
        if (esPagoDeServicio(producto)) {
            return "1111111111";
        }
        if (request.getTelefono() == null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST,
                    "Este producto requiere el campo 'telefono'");
        }
        return request.getTelefono();
    }

    private String resolverReferencia(GestoPagoProducto producto, CompraRequest request) {
        boolean esPagoDeServicio = esPagoDeServicio(producto);
        if (!esPagoDeServicio) {
            return null;
        }
        if (request.getReferencia() == null || request.getReferencia().isBlank()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST,
                    "Este producto requiere el campo 'referencia'");
        }
        return request.getReferencia();
    }

    private Cuenta buscarCuenta(String numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND,
                        "No existe una cuenta con el numero " + numeroCuenta));
    }

    private BigDecimal saldoVigente(Integer cuentaId) {
        return saldoRepository.findFirstByCuentaIdOrderByIdDesc(cuentaId)
                .map(Saldo::getMonto)
                .orElse(BigDecimal.ZERO);
    }

    private String generarCodigoVerificacion() {
        return String.format("%06d", generadorAleatorio.nextInt(1_000_000));
    }

    private String generarUpc() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 15);
    }

    private void guardarPendiente(String operacionId, CompraPendiente pendiente) {
        redisTemplate.opsForValue().set(clavePendiente(operacionId), escribirJson(pendiente), TTL_OPERACION);
    }

    private Optional<CompraPendiente> leerPendiente(String operacionId) {
        String json = redisTemplate.opsForValue().get(clavePendiente(operacionId));
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, CompraPendiente.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No fue posible leer la operacion de compra pendiente", e);
        }
    }

    /**
     * Se llama al llegar a un resultado DEFINITIVO (exito o rechazo claro de
     * GestoPago), nunca cuando el resultado queda indeterminado: en ese caso
     * el candado de "cuenta activa" debe seguir vigente hasta que expire
     * solo, para no permitir un segundo intento mientras no se sepa si el
     * primero ya se cobro.
     */
    private void liberarOperacion(String operacionId, Integer cuentaId) {
        redisTemplate.delete(clavePendiente(operacionId));
        redisTemplate.delete(claveActiva(cuentaId));
    }

    private String clavePendiente(String operacionId) {
        return PREFIJO_CLAVE_PENDIENTE + operacionId;
    }

    private String claveActiva(Integer cuentaId) {
        return PREFIJO_CLAVE_ACTIVA + cuentaId;
    }

    private String escribirJson(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No fue posible serializar el payload para GestoPago", e);
        }
    }
}
