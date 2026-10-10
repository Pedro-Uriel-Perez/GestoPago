package com.proyecto.servicios.repositorys.clientes;

import com.proyecto.servicios.entity.clientes.Saldo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SaldoRepository extends JpaRepository<Saldo, Integer> {

    Optional<Saldo> findFirstByCuentaIdOrderByFechaMovimientoDesc(Integer cuentaId);

    List<Saldo> findByCuentaIdOrderByFechaMovimientoDesc(Integer cuentaId);
}
