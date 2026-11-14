package com.proyecto.servicios.model.compras;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VerificarReferenciaRequest {

    @NotNull(message = "El idProducto es obligatorio")
    private Integer idProducto;

    @NotBlank(message = "La referencia es obligatoria")
    @Pattern(regexp = "^[A-Za-z0-9]{1,60}$",
            message = "La referencia solo puede contener letras y numeros (maximo 60 caracteres)")
    private String referencia;
}
