package com.proyecto.servicios.validation;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class EdadMinimaValidatorTest {

    private final EdadMinimaValidator validator = crearValidador(18);

    @Test
    void fechaDeHace19Anios_esValida() {
        LocalDate fecha = LocalDate.now().minusYears(19);

        assertThat(validator.isValid(fecha, null)).isTrue();
    }

    @Test
    void fechaDeHace18AniosExactos_esValida() {
        LocalDate fecha = LocalDate.now().minusYears(18);

        assertThat(validator.isValid(fecha, null)).isTrue();
    }

    @Test
    void fechaDeHace17Anios_noEsValida() {
        LocalDate fecha = LocalDate.now().minusYears(17);

        assertThat(validator.isValid(fecha, null)).isFalse();
    }

    @Test
    void unDiaAntesDeCumplir18_noEsValida() {
        LocalDate fecha = LocalDate.now().minusYears(18).plusDays(1);

        assertThat(validator.isValid(fecha, null)).isFalse();
    }

    @Test
    void fechaNula_esValida_elNotNullSeEncargaPorSeparado() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    private EdadMinimaValidator crearValidador(int edadMinima) {
        EdadMinimaValidator validador = new EdadMinimaValidator();
        validador.initialize(new EdadMinima() {
            @Override
            public int value() {
                return edadMinima;
            }

            @Override
            public String message() {
                return "";
            }

            @Override
            public Class<?>[] groups() {
                return new Class<?>[0];
            }

            @Override
            public Class<? extends jakarta.validation.Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return EdadMinima.class;
            }
        });
        return validador;
    }
}
