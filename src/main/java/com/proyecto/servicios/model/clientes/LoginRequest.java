package com.proyecto.servicios.model.clientes;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
}
