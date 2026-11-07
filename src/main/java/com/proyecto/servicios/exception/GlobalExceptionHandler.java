package com.proyecto.servicios.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.proyecto.servicios.model.GenericResponse;
import feign.FeignException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Punto unico de manejo de errores de la API.
 *
 * Todos los errores de negocio llegan como una sola excepcion,
 * NegocioException, que ya trae el status HTTP y el mensaje, y se atienden
 * en un solo metodo (handleNegocio). Los demas manejadores son para
 * errores que no lanza el codigo propio sino Spring o librerias externas
 * (validacion de campos, JSON mal formado, correo, Feign) y un catch-all
 * para que nunca se exponga un stack trace.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Para los 5xx no se reenvia el mensaje interno (puede traer datos de
     * configuracion o del proveedor externo): se registra en el log y al
     * cliente se le da un mensaje generico.
     */
    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<GenericResponse> handleNegocio(NegocioException ex) {
        HttpStatus status = ex.getStatus();
        if (status.is5xxServerError()) {
            log.error("Error de negocio {}: {}", status.value(), ex.getMessage());
            return construirRespuesta(status, "El servicio no esta disponible en este momento, intenta de nuevo mas tarde");
        }
        log.warn("Error de negocio {}: {}", status.value(), ex.getMessage());
        return construirRespuesta(status, ex.getMessage());
    }

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

    @ExceptionHandler(MailException.class)
    public ResponseEntity<GenericResponse> handleMailException(MailException ex) {
        log.error("No fue posible enviar el correo de verificacion: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.SERVICE_UNAVAILABLE,
                "No fue posible enviar el codigo de verificacion por correo, intenta de nuevo mas tarde");
    }

    /**
     * Errores de comunicacion con GestoPago que no se pudieron conciliar en
     * el service (ej. 401/403 por token rechazado). Se registra el detalle
     * en el log, pero al cliente nunca se le devuelve el cuerpo/URL del
     * proveedor.
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<GenericResponse> handleErrorProveedorExterno(FeignException ex) {
        log.error("Error de comunicacion con el proveedor externo: status={}", ex.status());
        return construirRespuesta(HttpStatus.BAD_GATEWAY,
                "El proveedor de pagos no respondio correctamente, intenta de nuevo mas tarde");
    }

    /**
     * Red de seguridad: ninguna excepcion no prevista debe devolver el stack
     * trace ni detalles internos. Las excepciones propias del framework
     * (404 de ruta inexistente, 405, 415, etc.) conservan su status.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleErrorInesperado(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatus status = Optional.ofNullable(HttpStatus.resolve(errorResponse.getStatusCode().value()))
                    .orElse(HttpStatus.INTERNAL_SERVER_ERROR);
            log.warn("Peticion rechazada por el framework: {} {}", status.value(), ex.getMessage());
            return construirRespuesta(status, status.getReasonPhrase());
        }
        log.error("Error inesperado procesando la peticion", ex);
        return construirRespuesta(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrio un error inesperado, intenta de nuevo mas tarde");
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
