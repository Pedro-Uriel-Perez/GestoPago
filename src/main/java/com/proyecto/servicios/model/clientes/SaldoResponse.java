package com.proyecto.servicios.model.clientes;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class SaldoResponse {

    private Integer id;
    private BigDecimal monto;
    private String tipoMovimiento;
    private LocalDateTime fechaMovimiento;
    private String descripcion;
}
