package com.proyecto.servicios.model.compras;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CompraResultadoResponse {

    private String numeroAutorizacion;

    private String idTransaccion;

    private String saldoRestante;

    private String mensaje;
}
