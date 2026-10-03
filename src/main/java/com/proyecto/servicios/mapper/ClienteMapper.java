package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.entity.clientes.Cuenta;
import com.proyecto.servicios.entity.clientes.Domicilio;
import com.proyecto.servicios.entity.clientes.Saldo;
import com.proyecto.servicios.model.clientes.ClienteActualizaRequest;
import com.proyecto.servicios.model.clientes.ClienteRequest;
import com.proyecto.servicios.model.clientes.ClienteResponse;
import com.proyecto.servicios.model.clientes.CuentaResponse;
import com.proyecto.servicios.model.clientes.DomicilioRequest;
import com.proyecto.servicios.model.clientes.DomicilioResponse;
import com.proyecto.servicios.model.clientes.SaldoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClienteMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaRegistro", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "nacionalidad", ignore = true)
    Cliente toEntity(ClienteRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "curp", ignore = true)
    @Mapping(target = "rfc", ignore = true)
    @Mapping(target = "fechaNacimiento", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaRegistro", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "nacionalidad", ignore = true)
    void actualizarEntity(ClienteActualizaRequest request, @MappingTarget Cliente cliente);

    @Mapping(target = "domicilio", ignore = true)
    @Mapping(target = "cuenta", ignore = true)
    @Mapping(target = "nacionalidadId", source = "nacionalidad.id")
    @Mapping(target = "nacionalidad", source = "nacionalidad.nombre")
    ClienteResponse toResponse(Cliente cliente);

    List<ClienteResponse> toResponseList(List<Cliente> clientes);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    Domicilio toEntity(DomicilioRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    void actualizarEntity(DomicilioRequest request, @MappingTarget Domicilio domicilio);

    DomicilioResponse toResponse(Domicilio domicilio);

    @Mapping(target = "saldoActual", ignore = true)
    CuentaResponse toResponse(Cuenta cuenta);

    SaldoResponse toResponse(Saldo saldo);

    List<SaldoResponse> toSaldoResponseList(List<Saldo> saldos);
}
