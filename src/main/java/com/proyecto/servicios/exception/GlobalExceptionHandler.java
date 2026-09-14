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
 * MethodArgumentNotValidException; las fallas de integracion con
 * servicios externos llegan como subclases de ProductListException.
 * Cada tipo de error se resuelve por su propio manejador (despacho por
 * tipo), sin necesidad de condicionales en los controllers/services.
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

    @ExceptionHandler(ProductListAuthenticationException.class)
    public ResponseEntity<GenericResponse> handleAuthenticationError(ProductListAuthenticationException ex) {
        log.error("Error de autenticacion con el servicio externo de productos: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(ProductListTimeoutException.class)
    public ResponseEntity<GenericResponse> handleTimeout(ProductListTimeoutException ex) {
        log.error("Timeout consumiendo el servicio externo de productos: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.GATEWAY_TIMEOUT, ex.getMessage());
    }

    @ExceptionHandler(ProductListUnsuccessfulResponseException.class)
    public ResponseEntity<GenericResponse> handleUnsuccessfulResponse(ProductListUnsuccessfulResponseException ex) {
        log.error("Respuesta no exitosa del servicio externo de productos: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(ProductListCommunicationException.class)
    public ResponseEntity<GenericResponse> handleCommunicationError(ProductListCommunicationException ex) {
        log.error("Error de comunicacion con el servicio externo de productos: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    private ResponseEntity<GenericResponse> construirRespuesta(HttpStatus status, String mensaje) {
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(status.value());
        respuesta.setMensaje(mensaje);
        return new ResponseEntity<>(respuesta, status);
    }
}
