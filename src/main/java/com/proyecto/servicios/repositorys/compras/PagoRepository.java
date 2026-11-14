package com.proyecto.servicios.repositorys.compras;

import com.proyecto.servicios.entity.compras.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Integer> {

    List<Pago> findByClienteIdOrderByIdDesc(Integer clienteId);

    Optional<Pago> findByIdAndClienteId(Integer id, Integer clienteId);
}
