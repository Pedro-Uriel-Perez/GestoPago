package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoSenTxClient;
import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.entity.clientes.Cuenta;
import com.proyecto.servicios.entity.clientes.Saldo;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.exception.ClienteInactivoException;
import com.proyecto.servicios.exception.CodigoVerificacionInvalidoException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.DatosCompraInvalidosException;
import com.proyecto.servicios.exception.GestoPagoSenTxRechazadoException;
import com.proyecto.servicios.exception.OperacionCompraNoEncontradaException;
import com.proyecto.servicios.exception.ProductoGestoPagoNoEncontradoException;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.model.compras.CompraConfirmarRequest;
import com.proyecto.servicios.model.compras.CompraIniciadaResponse;
import com.proyecto.servicios.model.compras.CompraPendiente;
import com.proyecto.servicios.model.compras.CompraRequest;
import com.proyecto.servicios.model.compras.CompraResultadoResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoSenTxRequest;
import com.proyecto.servicios.model.gestopago.GestoPagoSenTxResponse;
import com.proyecto.servicios.repositorys.clientes.CuentaRepository;
import com.proyecto.servicios.repositorys.clientes.SaldoRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.security.GestoPagoAesCipher;
import com.proyecto.servicios.service.CompraService;
import com.proyecto.servicios.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class CompraServiceImpl implements CompraService {

    private static final Duration TTL_OPERACION = Duration.ofMinutes(5);
    private static final DateTimeFormatter FORMATO_HORA_LOCAL = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String PREFIJO_CLAVE_REDIS = "compra:pendiente:";

    private final CuentaRepository cuentaRepository;
    private final SaldoRepository saldoRepository;
    private final GestoPagoProductoRepository productoRepository;
    private final GestoPagoSenTxClient senTxClient;
    private final GestoPagoAesCipher cipher;
    private final MailService mailService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SecureRandom generadorAleatorio = new SecureRandom();

    @Value("${gestopago.sendtx.unidad}")
    private String unidad;

    public CompraServiceImpl(CuentaRepository cuentaRepository,
                              SaldoRepository saldoRepository,
                              GestoPagoProductoRepository productoRepository,
                              GestoPagoSenTxClient senTxClient,
                              GestoPagoAesCipher cipher,
                              MailService mailService,
                              StringRedisTemplate redisTemplate,
                              ObjectMapper objectMapper) {
        this.cuentaRepository = cuentaRepository;
        this.saldoRepository = saldoRepository;
        this.productoRepository = productoRepository;
        this.senTxClient = senTxClient;
        this.cipher = cipher;
        this.mailService = mailService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public CompraIniciadaResponse iniciarCompra(CompraRequest request) {
        Cuenta cuenta = buscarCuenta(request.getNumeroCuenta());
        Cliente cliente = cuenta.getCliente();

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ClienteInactivoException("El cliente no esta activo, no puede realizar compras");
        }

        GestoPagoProducto producto = productoRepository.findByIdProducto(request.getIdProducto())
                .orElseThrow(() -> new ProductoGestoPagoNoEncontradoException(
                        "No existe un producto GestoPago con idProducto " + request.getIdProducto()));

        BigDecimal saldoActual = saldoVigente(cuenta.getId());
        if (saldoActual.compareTo(request.getMonto()) < 0) {
            throw new SaldoInsuficienteException("El saldo de la cuenta es insuficiente para esta compra");
        }

        String telefono = resolverTelefono(producto, request);
        String referencia = resolverReferencia(producto, request);

        String operacionId = UUID.randomUUID().toString();
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
                LocalDateTime.now().format(FORMATO_HORA_LOCAL),
                request.getMonto(),
                codigoVerificacion);

        guardarPendiente(operacionId, pendiente);
        mailService.enviarCodigoVerificacion(cliente.getCorreoElectronico(), codigoVerificacion);

        log.info("Compra iniciada, pendiente de verificacion 2FA. operacionId={}, clienteId={}",
                operacionId, cliente.getId());
        return new CompraIniciadaResponse(operacionId, "Se envio un codigo de verificacion a tu correo electronico");
    }

    @Override
    public CompraResultadoResponse confirmarCompra(CompraConfirmarRequest request) {
        CompraPendiente pendiente = leerPendiente(request.getOperacionId())
                .orElseThrow(() -> new OperacionCompraNoEncontradaException(
                        "La operacion no existe o ya expiro, inicia la compra nuevamente"));

        if (!pendiente.getCodigoVerificacion().equals(request.getCodigoVerificacion())) {
            throw new CodigoVerificacionInvalidoException("El codigo de verificacion no es correcto");
        }

        GestoPagoSenTxResponse respuesta = ejecutarSendTx(pendiente);

        log.info("sendTx GestoPago. operacionId={}, codigo={}, idTx={}",
                request.getOperacionId(), respuesta.getCodigo(), respuesta.getIdTx());

        if (!respuesta.esExitoso()) {
            String mensaje = respuesta.getTexto() != null
                    ? respuesta.getTexto()
                    : "GestoPago rechazo la transaccion (codigo " + respuesta.getCodigo() + ")";
            throw new GestoPagoSenTxRechazadoException(mensaje);
        }

        BigDecimal saldoRestante = registrarMovimientoSaldo(pendiente, respuesta);
        eliminarPendiente(request.getOperacionId());

        CompraResultadoResponse resultado = new CompraResultadoResponse();
        resultado.setNumeroAutorizacion(respuesta.getNumAutorizacion());
        resultado.setIdTransaccion(respuesta.getIdTx());
        resultado.setSaldoRestante(saldoRestante.toPlainString());
        resultado.setMensaje("Transaccion exitosa");
        return resultado;
    }

    private GestoPagoSenTxResponse ejecutarSendTx(CompraPendiente pendiente) {
        GestoPagoSenTxRequest payload = GestoPagoSenTxRequest.builder()
                .idProducto(pendiente.getIdProducto())
                .idServicio(pendiente.getIdServicio())
                .telefono(pendiente.getTelefono())
                .horaLocal(pendiente.getHoraLocal())
                .referencia(pendiente.getReferencia())
                .montoPago(pendiente.getMontoPago())
                .upc(pendiente.getUpc())
                .unidad(pendiente.getUnidad())
                .build();

        String signed = cipher.encriptar(escribirJson(payload));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("random", String.valueOf(System.currentTimeMillis()));
        form.add("signed", signed);

        return senTxClient.sendTx(form);
    }

    private BigDecimal registrarMovimientoSaldo(CompraPendiente pendiente, GestoPagoSenTxResponse respuesta) {
        Cuenta cuenta = cuentaRepository.findById(pendiente.getCuentaId())
                .orElseThrow(() -> new CuentaNoEncontradaException("La cuenta de la compra ya no existe"));

        BigDecimal saldoActual = saldoVigente(cuenta.getId());
        BigDecimal saldoNuevo = saldoActual.subtract(pendiente.getMonto());

        Saldo movimiento = new Saldo();
        movimiento.setCuenta(cuenta);
        movimiento.setMonto(saldoNuevo);
        movimiento.setTipoMovimiento("COMPRA");
        movimiento.setDescripcion("Compra GestoPago, autorizacion " + respuesta.getNumAutorizacion());
        saldoRepository.save(movimiento);

        return saldoNuevo;
    }

    private String resolverTelefono(GestoPagoProducto producto, CompraRequest request) {
        Integer idCatTipoServicio = producto.getIdCatTipoServicio();
        if (idCatTipoServicio != null && (idCatTipoServicio == 10 || idCatTipoServicio == 11)) {
            return "#1111111111";
        }
        if (Integer.valueOf(2).equals(producto.getTipoFront())) {
            return "1111111111";
        }
        if (request.getTelefono() == null) {
            throw new DatosCompraInvalidosException(
                    "Este producto requiere el campo 'telefono'");
        }
        return request.getTelefono();
    }

    private String resolverReferencia(GestoPagoProducto producto, CompraRequest request) {
        boolean esPagoDeServicio = Integer.valueOf(2).equals(producto.getTipoFront());
        if (!esPagoDeServicio) {
            return null;
        }
        if (request.getReferencia() == null || request.getReferencia().isBlank()) {
            throw new DatosCompraInvalidosException(
                    "Este producto requiere el campo 'referencia'");
        }
        return request.getReferencia();
    }

    private Cuenta buscarCuenta(String numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(
                        "No existe una cuenta con el numero " + numeroCuenta));
    }

    private BigDecimal saldoVigente(Integer cuentaId) {
        return saldoRepository.findFirstByCuentaIdOrderByFechaMovimientoDesc(cuentaId)
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

    private void eliminarPendiente(String operacionId) {
        redisTemplate.delete(clavePendiente(operacionId));
    }

    private String clavePendiente(String operacionId) {
        return PREFIJO_CLAVE_REDIS + operacionId;
    }

    private String escribirJson(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No fue posible serializar el payload para GestoPago", e);
        }
    }
}
