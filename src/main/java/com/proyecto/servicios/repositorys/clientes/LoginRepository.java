package com.proyecto.servicios.repositorys.clientes;

import com.proyecto.servicios.entity.clientes.Login;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoginRepository extends JpaRepository<Login, Integer> {

    Optional<Login> findByClienteId(Integer clienteId);
}
