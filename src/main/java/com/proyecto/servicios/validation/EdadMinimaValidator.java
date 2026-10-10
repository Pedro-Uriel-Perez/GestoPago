package com.proyecto.servicios.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

public class EdadMinimaValidator implements ConstraintValidator<EdadMinima, LocalDate> {

    private int edadMinima;

    @Override
    public void initialize(EdadMinima constraintAnnotation) {
        this.edadMinima = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(LocalDate fechaNacimiento, ConstraintValidatorContext context) {
        return Optional.ofNullable(fechaNacimiento)
                .map(fecha -> Period.between(fecha, LocalDate.now()).getYears() >= edadMinima)
                .orElse(true);
    }
}
