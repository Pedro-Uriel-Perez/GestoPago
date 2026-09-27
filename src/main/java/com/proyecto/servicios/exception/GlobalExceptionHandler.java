package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Error de validacion en el request: {}", mensaje);
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

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<GenericResponse> handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        log.warn("Intento de login con credenciales invalidas");
        return construirRespuesta(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    private ResponseEntity<GenericResponse> construirRespuesta(HttpStatus status, String mensaje) {
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(status.value());
        respuesta.setMensaje(mensaje);
        return new ResponseEntity<>(respuesta, status);
    }
}
