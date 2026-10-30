package com.proyecto.servicios.repositorys.compras;

import com.proyecto.servicios.entity.compras.ConciliacionPendiente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConciliacionPendienteRepository extends JpaRepository<ConciliacionPendiente, Integer> {
}
