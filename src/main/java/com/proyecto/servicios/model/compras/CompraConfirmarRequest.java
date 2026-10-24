package com.proyecto.servicios.model.compras;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CompraConfirmarRequest {

    @NotBlank(message = "El operacionId es obligatorio")
    private String operacionId;

    @NotBlank(message = "El codigo de verificacion es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "El codigo de verificacion debe contener exactamente 6 digitos")
    private String codigoVerificacion;
}
