package com.proyecto.servicios.model.clientes;

import com.proyecto.servicios.validation.EdadMinima;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class ClienteRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "El nombre solo puede contener letras y espacios")
    private String nombre;

    @Size(min = 2, max = 50, message = "El segundo nombre debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[\\p{L} ]*$", message = "El segundo nombre solo puede contener letras y espacios")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido paterno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "El apellido paterno solo puede contener letras y espacios")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido materno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "El apellido materno solo puede contener letras y espacios")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @PastOrPresent(message = "La fecha de nacimiento no puede ser una fecha futura")
    @EdadMinima(value = 18, message = "El cliente debe ser mayor de edad (18 anios o mas)")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "La CURP es obligatoria")
    @Pattern(
            regexp = "^[A-Z][AEIOU][A-Z]{2}\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])[HM](AS|BC|BS|CC|CL|CM|CS|CH|DF|DG|GT|GR|HG|JC|MC|MN|MS|NT|NL|OC|PL|QO|QR|SP|SL|SR|TC|TL|TS|VZ|YN|ZS|NE)[B-DF-HJ-NP-TV-Z]{3}[A-Z\\d]\\d$",
            message = "La CURP no tiene un formato valido")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Pattern(
            regexp = "^[A-ZÑ&]{3,4}\\d{6}[A-Z0-9]{3}$",
            message = "El RFC no tiene un formato valido")
    private String rfc;

    @NotBlank(message = "El sexo es obligatorio")
    @Pattern(regexp = "^(M|F)$", message = "El sexo debe ser 'M' o 'F'")
    private String sexo;

    @NotBlank(message = "La nacionalidad es obligatoria")
    private String nacionalidad;

    @NotBlank(message = "El estado civil es obligatorio")
    @Pattern(
            regexp = "^(SOLTERO|CASADO|DIVORCIADO|VIUDO|UNION_LIBRE)$",
            message = "El estado civil no es un valor valido")
    private String estadoCivil;

    @NotBlank(message = "El correo electronico es obligatorio")
    @Email(message = "El correo electronico no tiene un formato valido")
    @Size(max = 100, message = "El correo electronico no puede exceder 100 caracteres")
    private String correoElectronico;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 8, max = 100, message = "La contrasena debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "El telefono movil es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El telefono movil debe contener exactamente 10 digitos")
    private String telefonoMovil;

    @Pattern(regexp = "^\\d{10}$", message = "El telefono alternativo debe contener exactamente 10 digitos")
    private String telefonoAlternativo;

    @NotBlank(message = "La ocupacion es obligatoria")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    private BigDecimal ingresoMensual;

    @NotNull(message = "El domicilio es obligatorio")
    @Valid
    private DomicilioRequest domicilio;
}
