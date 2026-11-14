package com.proyecto.servicios.model.clientes;

import com.proyecto.servicios.validation.PatronesValidacion;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class DepositoRequest {

    @NotBlank(message = "El numero de cuenta es obligatorio")
    @Pattern(regexp = PatronesValidacion.NUMERO_CUENTA,
            message = "El numero de cuenta debe contener exactamente 10 digitos")
    private String numeroCuenta;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    @DecimalMax(value = "50000.00", message = "El monto maximo por deposito es de 50,000.00")
    @Digits(integer = 13, fraction = 2, message = "El monto solo puede tener hasta 2 decimales")
    private BigDecimal monto;
}
