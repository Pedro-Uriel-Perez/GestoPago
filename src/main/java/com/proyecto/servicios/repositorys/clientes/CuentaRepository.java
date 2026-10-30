package com.proyecto.servicios.repositorys.clientes;

import com.proyecto.servicios.entity.clientes.Cuenta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Integer> {

    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    Optional<Cuenta> findByClienteId(Integer clienteId);

    List<Cuenta> findByEstatus(String estatus);

    boolean existsByNumeroCuenta(String numeroCuenta);

    /**
     * Bloqueo pesimista (SELECT ... FOR UPDATE) sobre la fila de la cuenta.
     * Lo usa CompraServiceImpl.confirmarCompra para que dos confirmaciones
     * simultaneas de la MISMA cuenta no lean el mismo saldo "antes" y
     * ambas resten sobre el mismo valor (condicion de carrera / lost
     * update). Solo bloquea esa cuenta puntual, no toda la tabla, asi que
     * no afecta operaciones de otras cuentas en paralelo. Es un metodo de
     * repositorio derivado normal (findById), @Lock es solo una anotacion
     * de bloqueo, no una @Query.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Cuenta> findWithLockingById(Integer id);
}
