package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.clientes.Nacionalidad;
import com.proyecto.servicios.exception.NacionalidadNoEncontradaException;
import com.proyecto.servicios.model.clientes.NacionalidadResponse;
import com.proyecto.servicios.repositorys.clientes.NacionalidadRepository;
import com.proyecto.servicios.service.NacionalidadService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NacionalidadServiceImpl implements NacionalidadService {

    private final NacionalidadRepository nacionalidadRepository;

    public NacionalidadServiceImpl(NacionalidadRepository nacionalidadRepository) {
        this.nacionalidadRepository = nacionalidadRepository;
    }

    @Override
    public List<NacionalidadResponse> obtenerTodas() {
        return nacionalidadRepository.findAll().stream()
                .map(this::construirRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    public Nacionalidad buscarPorId(Integer id) {
        return nacionalidadRepository.findById(id)
                .orElseThrow(() -> new NacionalidadNoEncontradaException(id));
    }

    private NacionalidadResponse construirRespuesta(Nacionalidad nacionalidad) {
        NacionalidadResponse response = new NacionalidadResponse();
        response.setId(nacionalidad.getId());
        response.setNombre(nacionalidad.getNombre());
        return response;
    }
}
