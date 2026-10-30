package com.proyecto.servicios.model.compras;

import com.proyecto.servicios.validation.PatronesValidacion;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Cuerpo de POST /compras. "telefono" y "referencia" son opcionales a nivel
 * de Bean Validation a proposito: cual de los dos es obligatorio depende del
 * "tipoFront" del producto (idProducto), que solo se conoce tras consultar
 * el catalogo en base de datos, asi que esa regla se valida en
 * CompraServiceImpl, no aqui.
 */
@Getter
@Setter
@NoArgsConstructor
public class CompraRequest {

    @NotBlank(message = "El numero de cuenta es obligatorio")
    @Pattern(regexp = PatronesValidacion.NUMERO_CUENTA, message = "El numero de cuenta debe contener exactamente 10 digitos")
    private String numeroCuenta;

    @NotNull(message = "El idProducto es obligatorio")
    private Integer idProducto;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    private BigDecimal monto;

    @Pattern(regexp = "^\\d{10}$", message = "El telefono debe contener exactamente 10 digitos")
    private String telefono;

    private String referencia;
}
