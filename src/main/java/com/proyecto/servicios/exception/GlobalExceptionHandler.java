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

    private ResponseEntity<GenericResponse> construirRespuesta(HttpStatus status, String mensaje) {
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(status.value());
        respuesta.setMensaje(mensaje);
        return new ResponseEntity<>(respuesta, status);
    }
}
