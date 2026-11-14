package com.proyecto.servicios.model.clientes;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "El correo electronico es obligatorio")
    @Email(message = "El correo electronico no tiene un formato valido")
    private String correoElectronico;

    @NotBlank(message = "La contrasena es obligatoria")
    private String password;

    /**
     * Desde donde se inicia sesion: WEB o MOVIL. Opcional (por defecto WEB).
     * Cada dispositivo mantiene su propia sesion.
     */
    @Pattern(regexp = "^(WEB|MOVIL)$", message = "El dispositivo debe ser WEB o MOVIL")
    private String dispositivo;
}
