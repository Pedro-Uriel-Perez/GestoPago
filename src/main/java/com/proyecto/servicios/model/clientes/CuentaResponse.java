package com.proyecto.servicios.model.clientes;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CuentaResponse {

    private Integer id;
    private String numeroCuenta;
    private String estatus;
    private LocalDateTime fechaApertura;
    private BigDecimal saldoActual;
}
