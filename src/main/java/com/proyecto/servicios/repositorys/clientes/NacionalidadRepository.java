package com.proyecto.servicios.repositorys.clientes;

import com.proyecto.servicios.entity.clientes.Nacionalidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NacionalidadRepository extends JpaRepository<Nacionalidad, Integer> {
}
