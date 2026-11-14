package com.proyecto.servicios.repositorys.clientes;

import com.proyecto.servicios.entity.clientes.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SesionRepository extends JpaRepository<Sesion, Integer> {

    Optional<Sesion> findByIdentificador(String identificador);

    List<Sesion> findByClienteIdAndActivaTrueOrderByFechaInicioDesc(Integer clienteId);

    List<Sesion> findByActivaTrueAndFechaUltimoAccesoBefore(LocalDateTime limite);
}
