package com.proyecto.servicios.repositorys.compras;

import com.proyecto.servicios.entity.compras.BitacoraGestoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BitacoraGestoPagoRepository extends JpaRepository<BitacoraGestoPago, Integer> {
}
