package com.proyecto.servicios.repositorys.clientes;

import com.proyecto.servicios.entity.clientes.Domicilio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DomicilioRepository extends JpaRepository<Domicilio, Integer> {

    Optional<Domicilio> findByClienteId(Integer clienteId);
}
