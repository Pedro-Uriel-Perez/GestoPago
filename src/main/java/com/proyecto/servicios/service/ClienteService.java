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

    ClienteResponse actualizarCliente(Integer id, ClienteActualizaRequest request);

    void eliminarCliente(Integer id);

    List<ClienteResponse> obtenerActivos();

    List<ClienteResponse> obtenerPorRangoFechas(LocalDateTime desde, LocalDateTime hasta);
}
