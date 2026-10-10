package com.proyecto.servicios.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.proyecto.servicios.model.GenericResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidationErrors_unoOVariosCamposInvalidos_devuelve400ConLosMensajes() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "clienteRequest");
        bindingResult.addError(new FieldError("clienteRequest", "nombre", "El nombre es obligatorio"));
        bindingResult.addError(new FieldError("clienteRequest", "correoElectronico", "El correo no es valido"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                mock(org.springframework.core.MethodParameter.class), bindingResult);

        ResponseEntity<GenericResponse> respuesta = handler.handleValidationErrors(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().getMensaje())
                .contains("El nombre es obligatorio")
                .contains("El correo no es valido");
    }

    @Test
    void handleJsonNoLegible_fechaConFormatoInvalido_devuelve400ConElCampoYElValor() {
        InvalidFormatException causa = InvalidFormatException.from(
                null, "no se pudo parsear", "1999-11/08", LocalDate.class);
        causa.prependPath(Object.class, "fechaNacimiento");
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", causa, null);

        ResponseEntity<GenericResponse> respuesta = handler.handleJsonNoLegible(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().getMensaje())
                .contains("fechaNacimiento")
                .contains("1999-11/08");
    }

    @Test
    void handleJsonNoLegible_numeroMandadoComoTexto_devuelve400MencionandoElCampo() {
        MismatchedInputException causa = MismatchedInputException.from(null, java.math.BigDecimal.class,
                "Cannot coerce String value (\"19000\") to BigDecimal");
        causa.prependPath(Object.class, "ingresoMensual");
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", causa, null);

        ResponseEntity<GenericResponse> respuesta = handler.handleJsonNoLegible(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().getMensaje()).contains("ingresoMensual");
    }

    @Test
    void handleJsonNoLegible_sinCausaReconocida_devuelveMensajeGenerico() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("cuerpo vacio o invalido");

        ResponseEntity<GenericResponse> respuesta = handler.handleJsonNoLegible(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().getMensaje()).isEqualTo("El cuerpo de la peticion tiene un formato invalido");
    }

    @Test
    void handleClienteYaRegistrado_devuelve409() {
        ResponseEntity<GenericResponse> respuesta = handler.handleClienteYaRegistrado(
                new ClienteYaRegistradoException("Ya existe un cliente registrado con el correo x@x.com"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody().getMensaje()).contains("x@x.com");
    }

    @Test
    void handleClienteNoEncontrado_devuelve404() {
        ResponseEntity<GenericResponse> respuesta = handler.handleClienteNoEncontrado(
                new ClienteNoEncontradoException("No existe un cliente con id 99"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleCuentaNoEncontrada_devuelve404() {
        ResponseEntity<GenericResponse> respuesta = handler.handleCuentaNoEncontrada(
                new CuentaNoEncontradaException("No existe una cuenta con el numero 0000000000"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleNacionalidadNoEncontrada_devuelve404() {
        ResponseEntity<GenericResponse> respuesta = handler.handleNacionalidadNoEncontrada(
                new NacionalidadNoEncontradaException(999));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody().getMensaje()).contains("999");
    }

    @Test
    void handleConstraintViolation_parametroDeQueryInvalido_devuelve400ConLosMensajes() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("El correo electronico no tiene un formato valido");
        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<GenericResponse> respuesta = handler.handleConstraintViolation(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().getMensaje()).contains("correo electronico");
    }

    @Test
    void handleTipoDeParametroInvalido_devuelve400ConElNombreYElValor() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "no-es-un-id", Integer.class, "id", mock(MethodParameter.class), null);

        ResponseEntity<GenericResponse> respuesta = handler.handleTipoDeParametroInvalido(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().getMensaje())
                .contains("id")
                .contains("no-es-un-id");
    }

    @Test
    void handleCriterioBusquedaInvalido_devuelve400() {
        ResponseEntity<GenericResponse> respuesta = handler.handleCriterioBusquedaInvalido(
                new CriterioBusquedaInvalidoException("Debes proporcionar exactamente un criterio de busqueda"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().getMensaje()).contains("exactamente un criterio");
    }

    @Test
    void handleCredencialesInvalidas_devuelve401() {
        ResponseEntity<GenericResponse> respuesta = handler.handleCredencialesInvalidas(
                new CredencialesInvalidasException("Correo o contrasena incorrectos"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta.getBody().getMensaje()).isEqualTo("Correo o contrasena incorrectos");
    }
}
