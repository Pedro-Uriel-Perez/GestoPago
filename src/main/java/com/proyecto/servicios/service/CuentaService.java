package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.clientes.Cuenta;
import com.proyecto.servicios.model.clientes.CuentaResponse;
import com.proyecto.servicios.model.clientes.SaldoResponse;

import java.util.List;

public interface CuentaService {

    CuentaResponse obtenerPorNumeroCuenta(String numeroCuenta);

    List<CuentaResponse> obtenerActivas();

    List<SaldoResponse> obtenerHistorialSaldo(String numeroCuenta);

    /**
     * Ensambla el CuentaResponse (incluyendo el saldo actual) a partir de
     * una entidad ya cargada. Publico para que ClienteServiceImpl lo
     * reutilice al armar la respuesta completa de un cliente, sin duplicar
     * la logica de "buscar el saldo mas reciente".
     */
    CuentaResponse construirRespuesta(Cuenta cuenta);
}
