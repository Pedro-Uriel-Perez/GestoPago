package com.proyecto.servicios.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.proyecto.servicios.model.GenericResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Punto unico de manejo de errores de la API. Las validaciones de los
 * *Request se declaran con Bean Validation y llegan aqui como
 * MethodArgumentNotValidException, sin necesidad de condicionales en los
 * controllers/services.
 *
 * Las excepciones ProductList*Exception (autenticacion, timeout, respuesta
 * no exitosa, comunicacion) no se manejan aqui: ocurren dentro del job
 * programado GestoPagoProductListSyncServiceImpl, fuera de una peticion
 * HTTP, y se registran (log) ahi mismo.
 *
 * Las excepciones de Onboarding de Clientes (Cliente/Cuenta no encontrado,
 * CURP/RFC/correo duplicado) si se manejan aqui, porque ocurren dentro de
 * una peticion HTTP normal (ClienteController/CuentaController).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errores.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("Error de validacion en el request: {}", errores);

        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(1);
        respuesta.setMensaje("Datos no validos" + errores);
        return new ResponseEntity<>(respuesta, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse> handleJsonNoLegible(HttpMessageNotReadableException ex) {
        String mensaje = Optional.ofNullable(ex.getCause())
                .filter(JsonMappingException.class::isInstance)
                .map(JsonMappingException.class::cast)
                .map(this::mensajeDeCampoInvalido)
                .orElse("El cuerpo de la peticion tiene un formato invalido");
        log.warn("JSON invalido en el request: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GenericResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String mensaje = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("Parametro invalido en el request: {}", mensaje);
        return construirRespuesta(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GenericResponse> handleTipoDeParametroInvalido(MethodArgumentTypeMismatchException ex) {
        String mensaje = "El parametro '" + ex.getName() + "' tiene un valor invalido: '" + ex.getValue() + "'";
        log.warn("Tipo de parametro invalido en el request: {}", mensaje);
        return construirRespuesta(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(ClienteYaRegistradoException.class)
    public ResponseEntity<GenericResponse> handleClienteYaRegistrado(ClienteYaRegistradoException ex) {
        log.warn("Intento de registrar un cliente duplicado: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<GenericResponse> handleClienteNoEncontrado(ClienteNoEncontradoException ex) {
        log.warn("Cliente no encontrado: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CuentaNoEncontradaException.class)
    public ResponseEntity<GenericResponse> handleCuentaNoEncontrada(CuentaNoEncontradaException ex) {
        log.warn("Cuenta no encontrada: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(NacionalidadNoEncontradaException.class)
    public ResponseEntity<GenericResponse> handleNacionalidadNoEncontrada(NacionalidadNoEncontradaException ex) {
        log.warn("Nacionalidad no encontrada: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<GenericResponse> handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        log.warn("Intento de login con credenciales invalidas");
        return construirRespuesta(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(ClienteInactivoException.class)
    public ResponseEntity<GenericResponse> handleClienteInactivo(ClienteInactivoException ex) {
        log.warn("Operacion rechazada, cliente inactivo: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(SaldoInsuficienteException.class)
    public ResponseEntity<GenericResponse> handleSaldoInsuficiente(SaldoInsuficienteException ex) {
        log.warn("Saldo insuficiente: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ProductoGestoPagoNoEncontradoException.class)
    public ResponseEntity<GenericResponse> handleProductoGestoPagoNoEncontrado(ProductoGestoPagoNoEncontradoException ex) {
        log.warn("Producto GestoPago no encontrado: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(OperacionCompraNoEncontradaException.class)
    public ResponseEntity<GenericResponse> handleOperacionCompraNoEncontrada(OperacionCompraNoEncontradaException ex) {
        log.warn("Operacion de compra no encontrada o expirada: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CodigoVerificacionInvalidoException.class)
    public ResponseEntity<GenericResponse> handleCodigoVerificacionInvalido(CodigoVerificacionInvalidoException ex) {
        log.warn("Codigo de verificacion (2FA) invalido: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(GestoPagoSenTxRechazadoException.class)
    public ResponseEntity<GenericResponse> handleGestoPagoSenTxRechazado(GestoPagoSenTxRechazadoException ex) {
        log.warn("GestoPago rechazo la transaccion: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DatosCompraInvalidosException.class)
    public ResponseEntity<GenericResponse> handleDatosCompraInvalidos(DatosCompraInvalidosException ex) {
        log.warn("Datos de compra invalidos: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MailException.class)
    public ResponseEntity<GenericResponse> handleMailException(MailException ex) {
        log.error("No fue posible enviar el correo de verificacion: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.SERVICE_UNAVAILABLE,
                "No fue posible enviar el codigo de verificacion por correo, intenta de nuevo mas tarde");
    }

    private String mensajeDeCampoInvalido(JsonMappingException ex) {
        String campo = ex.getPath().isEmpty()
                ? "desconocido"
                : ex.getPath().get(ex.getPath().size() - 1).getFieldName();

        return Optional.of(ex)
                .filter(InvalidFormatException.class::isInstance)
                .map(InvalidFormatException.class::cast)
                .map(ife -> "El campo '" + campo + "' tiene un formato invalido: '" + ife.getValue() + "'")
                .orElse("El campo '" + campo + "' tiene un valor invalido");
    }

    private ResponseEntity<GenericResponse> construirRespuesta(HttpStatus status, String mensaje) {
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(status.value());
        respuesta.setMensaje(mensaje);
        return new ResponseEntity<>(respuesta, status);
    }
}
