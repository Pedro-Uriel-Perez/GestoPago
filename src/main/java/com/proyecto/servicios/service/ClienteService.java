package com.proyecto.servicios.service;

import com.proyecto.servicios.model.clientes.ClienteActualizaRequest;
import com.proyecto.servicios.model.clientes.ClienteBusquedaRequest;
import com.proyecto.servicios.model.clientes.ClienteRequest;
import com.proyecto.servicios.model.clientes.ClienteResponse;

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
     * Resuelve POST /clientes/buscar: combina con AND solo los criterios
     * que vienen distintos de null en el filtro (curp, rfc, correo,
     * numeroCuenta, activo, rango de fecha de registro). Si no coincide
     * ningun cliente devuelve una lista vacia, no un 404.
     */
    List<ClienteResponse> buscarClientes(ClienteBusquedaRequest filtro);

    ClienteResponse actualizarCliente(Integer id, ClienteActualizaRequest request);

    void eliminarCliente(Integer id);
}
