package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.clientes.Nacionalidad;
import com.proyecto.servicios.model.clientes.NacionalidadResponse;

import java.util.List;

public interface NacionalidadService {

    List<NacionalidadResponse> obtenerTodas();

    /**
     * Busca la nacionalidad por id o lanza NacionalidadNoEncontradaException.
     * Publico para que ClienteServiceImpl lo reutilice al armar/actualizar
     * un cliente, sin duplicar la logica de busqueda.
     */
    Nacionalidad buscarPorId(Integer id);
}
