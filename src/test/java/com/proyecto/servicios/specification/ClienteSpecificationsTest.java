package com.proyecto.servicios.specification;

import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.model.clientes.ClienteBusquedaRequest;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteSpecificationsTest {

    @Mock
    private Root<Cliente> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder builder;

    @Mock
    private Path<Object> path;

    @Test
    void of_sinFiltros_combinaUnAndVacioQueTraeTodos() {
        ClienteSpecifications.of(new ClienteBusquedaRequest()).toPredicate(root, query, builder);

        ArgumentCaptor<Predicate[]> condiciones = ArgumentCaptor.forClass(Predicate[].class);
        verify(builder).and(condiciones.capture());
        assertThat(condiciones.getValue()).isEmpty();
        verify(root, never()).get(anyString());
    }

    @Test
    void of_conTodosLosFiltros_agregaUnaCondicionPorCampo() {
        LocalDateTime desde = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 12, 31, 23, 59);
        ClienteBusquedaRequest filtro = new ClienteBusquedaRequest();
        filtro.setCurp("PEMP000101HDFRNDA1");
        filtro.setRfc("PEMP000101AB1");
        filtro.setCorreoElectronico("pedro@correo.com");
        filtro.setActivo(true);
        filtro.setFechaRegistroDesde(desde);
        filtro.setFechaRegistroHasta(hasta);
        when(root.get(anyString())).thenReturn(path);

        ClienteSpecifications.of(filtro).toPredicate(root, query, builder);

        verify(builder).equal(path, "PEMP000101HDFRNDA1");
        verify(builder).equal(path, "PEMP000101AB1");
        verify(builder).equal(path, "pedro@correo.com");
        verify(builder).equal(path, true);
        verify(builder).greaterThanOrEqualTo(any(), eq(desde));
        verify(builder).lessThanOrEqualTo(any(), eq(hasta));
        verify(root).get("curp");
        verify(root).get("rfc");
        verify(root).get("correoElectronico");
        verify(root).get("activo");
        verify(root, times(2)).get("fechaRegistro");

        ArgumentCaptor<Predicate[]> condiciones = ArgumentCaptor.forClass(Predicate[].class);
        verify(builder).and(condiciones.capture());
        assertThat(condiciones.getValue()).hasSize(6);
    }

    @Test
    void of_soloCurp_noFiltraPorLosDemasCampos() {
        ClienteBusquedaRequest filtro = new ClienteBusquedaRequest();
        filtro.setCurp("PEMP000101HDFRNDA1");
        when(root.get("curp")).thenReturn(path);

        ClienteSpecifications.of(filtro).toPredicate(root, query, builder);

        verify(builder).equal(path, "PEMP000101HDFRNDA1");
        verify(root, never()).get("rfc");
        verify(root, never()).get("fechaRegistro");
        ArgumentCaptor<Predicate[]> condiciones = ArgumentCaptor.forClass(Predicate[].class);
        verify(builder).and(condiciones.capture());
        assertThat(condiciones.getValue()).hasSize(1);
    }
}
