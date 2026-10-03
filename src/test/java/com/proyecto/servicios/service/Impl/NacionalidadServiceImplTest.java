package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.clientes.Nacionalidad;
import com.proyecto.servicios.exception.NacionalidadNoEncontradaException;
import com.proyecto.servicios.model.clientes.NacionalidadResponse;
import com.proyecto.servicios.repositorys.clientes.NacionalidadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NacionalidadServiceImplTest {

    @Mock
    private NacionalidadRepository nacionalidadRepository;

    @InjectMocks
    private NacionalidadServiceImpl nacionalidadService;

    @Test
    void obtenerTodas_devuelveTodasLasNacionalidadesDelCatalogo() {
        Nacionalidad mexicana = new Nacionalidad();
        mexicana.setId(1);
        mexicana.setNombre("MEXICANA");

        Nacionalidad estadounidense = new Nacionalidad();
        estadounidense.setId(2);
        estadounidense.setNombre("ESTADOUNIDENSE");

        when(nacionalidadRepository.findAll()).thenReturn(List.of(mexicana, estadounidense));

        List<NacionalidadResponse> resultado = nacionalidadService.obtenerTodas();

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(NacionalidadResponse::getNombre)
                .containsExactlyInAnyOrder("MEXICANA", "ESTADOUNIDENSE");
    }

    @Test
    void buscarPorId_idExistente_devuelveLaNacionalidad() {
        Nacionalidad mexicana = new Nacionalidad();
        mexicana.setId(1);
        mexicana.setNombre("MEXICANA");
        when(nacionalidadRepository.findById(1)).thenReturn(Optional.of(mexicana));

        Nacionalidad resultado = nacionalidadService.buscarPorId(1);

        assertThat(resultado.getNombre()).isEqualTo("MEXICANA");
    }

    @Test
    void buscarPorId_idNoExiste_lanzaNacionalidadNoEncontradaException() {
        when(nacionalidadRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> nacionalidadService.buscarPorId(99))
                .isInstanceOf(NacionalidadNoEncontradaException.class);
    }
}
