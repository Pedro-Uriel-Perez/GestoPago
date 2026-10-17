package com.proyecto.servicios.specification;

import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.model.clientes.ClienteBusquedaRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Construye un Specification<Cliente> combinando con AND solo los campos de
 * ClienteBusquedaRequest que vienen distintos de null, para permitir
 * filtros combinables (POST /clientes/buscar) sin un metodo de repositorio
 * por cada combinacion posible.
 */
public final class ClienteSpecifications {

    private ClienteSpecifications() {
    }

    public static Specification<Cliente> of(ClienteBusquedaRequest filtro) {
        return (root, query, builder) -> {
            List<jakarta.persistence.criteria.Predicate> condiciones = new ArrayList<>();

            if (filtro.getCurp() != null) {
                condiciones.add(builder.equal(root.get("curp"), filtro.getCurp()));
            }
            if (filtro.getRfc() != null) {
                condiciones.add(builder.equal(root.get("rfc"), filtro.getRfc()));
            }
            if (filtro.getCorreoElectronico() != null) {
                condiciones.add(builder.equal(root.get("correoElectronico"), filtro.getCorreoElectronico()));
            }
            if (filtro.getActivo() != null) {
                condiciones.add(builder.equal(root.get("activo"), filtro.getActivo()));
            }
            if (filtro.getFechaRegistroDesde() != null) {
                condiciones.add(builder.greaterThanOrEqualTo(root.get("fechaRegistro"), filtro.getFechaRegistroDesde()));
            }
            if (filtro.getFechaRegistroHasta() != null) {
                condiciones.add(builder.lessThanOrEqualTo(root.get("fechaRegistro"), filtro.getFechaRegistroHasta()));
            }

            return builder.and(condiciones.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
