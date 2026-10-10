package com.proyecto.servicios.service;

import com.proyecto.servicios.model.clientes.ClienteActualizaRequest;
import com.proyecto.servicios.model.clientes.ClienteRequest;
import com.proyecto.servicios.model.clientes.ClienteResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface ClienteService {

    ClienteResponse crearCliente(ClienteRequest request);

    List<ClienteResponse> obtenerTodos();

    ClienteResponse obtenerPorId(Integer id);

    ClienteResponse obtenerPorCurp(String curp);

    ClienteResponse obtenerPorRfc(String rfc);

    ClienteResponse obtenerPorCorreo(String correo);

    ClienteResponse obtenerPorNumeroCuenta(String numeroCuenta);

    /**
     * Resuelve GET /clientes/buscar: exige que se haya mandado exactamente
     * uno de los cuatro parametros (curp, rfc, correo, numeroCuenta) y
     * delega en el metodo correspondiente. Lanza
     * CriterioBusquedaInvalidoException (400) si se mandan cero o varios a
     * la vez.
     */
    ClienteResponse buscarPorCriterio(String curp, String rfc, String correo, String numeroCuenta);

    ClienteResponse actualizarCliente(Integer id, ClienteActualizaRequest request);

    void eliminarCliente(Integer id);

    List<ClienteResponse> obtenerActivos();

    List<ClienteResponse> obtenerPorRangoFechas(LocalDateTime desde, LocalDateTime hasta);
}
