package com.proyecto.servicios.model.clientes;

import com.proyecto.servicios.validation.PatronesValidacion;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Cuerpo de POST /clientes/buscar. Todos los campos son opcionales y se
 * combinan con AND: el cliente que se devuelve (o los clientes, en el caso
 * del rango de fechas) debe cumplir todos los criterios que se hayan
 * mandado. Bean Validation (@Pattern/@Email) no evalua un campo si viene
 * null, asi que un campo ausente simplemente no se usa como filtro.
 */
@Getter
@Setter
@NoArgsConstructor
public class ClienteBusquedaRequest {

    @Pattern(regexp = PatronesValidacion.CURP, message = "La CURP no tiene un formato valido")
    private String curp;

    @Pattern(regexp = PatronesValidacion.RFC, message = "El RFC no tiene un formato valido")
    private String rfc;

    @Email(message = "El correo electronico no tiene un formato valido")
    private String correoElectronico;

    @Pattern(regexp = PatronesValidacion.NUMERO_CUENTA, message = "El numero de cuenta debe contener exactamente 10 digitos")
    private String numeroCuenta;

    private Boolean activo;

    private LocalDateTime fechaRegistroDesde;

    private LocalDateTime fechaRegistroHasta;
}
