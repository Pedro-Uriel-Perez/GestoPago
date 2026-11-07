package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.entity.clientes.Cuenta;
import com.proyecto.servicios.entity.clientes.Domicilio;
import com.proyecto.servicios.entity.clientes.Nacionalidad;
import com.proyecto.servicios.entity.clientes.Saldo;
import com.proyecto.servicios.model.clientes.ClienteActualizaRequest;
import com.proyecto.servicios.model.clientes.ClienteRequest;
import com.proyecto.servicios.model.clientes.ClienteResponse;
import com.proyecto.servicios.model.clientes.CuentaResponse;
import com.proyecto.servicios.model.clientes.DomicilioRequest;
import com.proyecto.servicios.model.clientes.DomicilioResponse;
import com.proyecto.servicios.model.clientes.SaldoResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClienteMapperImplTest {

    private final ClienteMapper mapper = new ClienteMapperImpl();

    private static DomicilioRequest domicilioRequest() {
        DomicilioRequest request = new DomicilioRequest();
        request.setCalle("Reforma");
        request.setNumeroExterior("10");
        request.setNumeroInterior("2B");
        request.setColonia("Centro");
        request.setMunicipio("Cuauhtemoc");
        request.setEstado("CDMX");
        request.setCodigoPostal("06000");
        request.setPais("Mexico");
        return request;
    }

    private static Cliente cliente() {
        Nacionalidad nacionalidad = new Nacionalidad();
        nacionalidad.setId(1);
        nacionalidad.setNombre("Mexicana");

        Cliente cliente = new Cliente();
        cliente.setId(7);
        cliente.setNombre("Pedro");
        cliente.setApellidoPaterno("Perez");
        cliente.setApellidoMaterno("Monzon");
        cliente.setCurp("PEMP000101HDFRNDA1");
        cliente.setRfc("PEMP000101AB1");
        cliente.setCorreoElectronico("pedro@correo.com");
        cliente.setIngresoMensual(new BigDecimal("15000.00"));
        cliente.setNacionalidad(nacionalidad);
        return cliente;
    }

    @Test
    void toEntity_clienteRequest_copiaLosDatosPersonales() {
        ClienteRequest request = new ClienteRequest();
        request.setNombre("Pedro");
        request.setSegundoNombre("Uriel");
        request.setApellidoPaterno("Perez");
        request.setApellidoMaterno("Monzon");
        request.setFechaNacimiento(LocalDate.of(2000, 1, 1));
        request.setCurp("PEMP000101HDFRNDA1");
        request.setRfc("PEMP000101AB1");
        request.setSexo("M");
        request.setEstadoCivil("SOLTERO");
        request.setCorreoElectronico("pedro@correo.com");
        request.setTelefonoMovil("5512345678");
        request.setTelefonoAlternativo("5587654321");
        request.setOcupacion("Estudiante");
        request.setEmpresa("UT");
        request.setIngresoMensual(new BigDecimal("15000.00"));

        Cliente cliente = mapper.toEntity(request);

        assertThat(cliente.getNombre()).isEqualTo("Pedro");
        assertThat(cliente.getSegundoNombre()).isEqualTo("Uriel");
        assertThat(cliente.getCurp()).isEqualTo("PEMP000101HDFRNDA1");
        assertThat(cliente.getFechaNacimiento()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(cliente.getIngresoMensual()).isEqualByComparingTo("15000.00");
        assertThat(cliente.getId()).isNull();
        assertThat(cliente.getNacionalidad()).isNull();
    }

    @Test
    void actualizarEntity_clienteActualizaRequest_noTocaCurpNiRfc() {
        Cliente cliente = cliente();
        ClienteActualizaRequest request = new ClienteActualizaRequest();
        request.setNombre("Juan");
        request.setApellidoPaterno("Lopez");
        request.setApellidoMaterno("Garcia");
        request.setSexo("M");
        request.setEstadoCivil("CASADO");
        request.setCorreoElectronico("juan@correo.com");
        request.setTelefonoMovil("5511111111");
        request.setOcupacion("Ingeniero");
        request.setEmpresa("ACME");
        request.setIngresoMensual(new BigDecimal("20000.00"));

        mapper.actualizarEntity(request, cliente);

        assertThat(cliente.getNombre()).isEqualTo("Juan");
        assertThat(cliente.getCorreoElectronico()).isEqualTo("juan@correo.com");
        assertThat(cliente.getCurp()).isEqualTo("PEMP000101HDFRNDA1");
        assertThat(cliente.getRfc()).isEqualTo("PEMP000101AB1");
        assertThat(cliente.getId()).isEqualTo(7);
    }

    @Test
    void toResponse_cliente_incluyeIdYNombreDeNacionalidad() {
        ClienteResponse response = mapper.toResponse(cliente());

        assertThat(response.getId()).isEqualTo(7);
        assertThat(response.getNombre()).isEqualTo("Pedro");
        assertThat(response.getNacionalidadId()).isEqualTo(1);
        assertThat(response.getNacionalidad()).isEqualTo("Mexicana");
        assertThat(response.getDomicilio()).isNull();
        assertThat(response.getCuenta()).isNull();
    }

    @Test
    void toResponse_clienteSinNacionalidad_dejaNacionalidadEnNull() {
        Cliente cliente = cliente();
        cliente.setNacionalidad(null);

        ClienteResponse response = mapper.toResponse(cliente);

        assertThat(response.getNacionalidadId()).isNull();
        assertThat(response.getNacionalidad()).isNull();
    }

    @Test
    void toResponseList_mapeaCadaCliente() {
        List<ClienteResponse> responses = mapper.toResponseList(List.of(cliente(), cliente()));

        assertThat(responses).hasSize(2).allSatisfy(r -> assertThat(r.getNombre()).isEqualTo("Pedro"));
    }

    @Test
    void toEntity_domicilioRequest_copiaLaDireccion() {
        Domicilio domicilio = mapper.toEntity(domicilioRequest());

        assertThat(domicilio.getCalle()).isEqualTo("Reforma");
        assertThat(domicilio.getNumeroInterior()).isEqualTo("2B");
        assertThat(domicilio.getCodigoPostal()).isEqualTo("06000");
        assertThat(domicilio.getPais()).isEqualTo("Mexico");
        assertThat(domicilio.getId()).isNull();
    }

    @Test
    void actualizarEntity_domicilio_reemplazaLaDireccion() {
        Domicilio domicilio = new Domicilio();
        domicilio.setId(3);
        domicilio.setCalle("Vieja");

        mapper.actualizarEntity(domicilioRequest(), domicilio);

        assertThat(domicilio.getCalle()).isEqualTo("Reforma");
        assertThat(domicilio.getMunicipio()).isEqualTo("Cuauhtemoc");
        assertThat(domicilio.getId()).isEqualTo(3);
    }

    @Test
    void toResponse_domicilio_copiaLaDireccion() {
        Domicilio domicilio = mapper.toEntity(domicilioRequest());
        domicilio.setId(3);

        DomicilioResponse response = mapper.toResponse(domicilio);

        assertThat(response.getId()).isEqualTo(3);
        assertThat(response.getCalle()).isEqualTo("Reforma");
        assertThat(response.getEstado()).isEqualTo("CDMX");
    }

    @Test
    void toResponse_cuenta_noCalculaElSaldo() {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(4);
        cuenta.setNumeroCuenta("1234567890");
        cuenta.setFechaApertura(LocalDateTime.of(2026, 1, 1, 10, 0));

        CuentaResponse response = mapper.toResponse(cuenta);

        assertThat(response.getId()).isEqualTo(4);
        assertThat(response.getNumeroCuenta()).isEqualTo("1234567890");
        assertThat(response.getEstatus()).isEqualTo("ACTIVA");
        assertThat(response.getSaldoActual()).isNull();
    }

    @Test
    void toSaldoResponseList_mapeaCadaMovimiento() {
        Saldo saldo = new Saldo();
        saldo.setId(9);
        saldo.setMonto(new BigDecimal("500.00"));
        saldo.setTipoMovimiento("DEPOSITO");
        saldo.setDescripcion("Deposito inicial");

        List<SaldoResponse> responses = mapper.toSaldoResponseList(List.of(saldo));

        assertThat(responses).singleElement().satisfies(r -> {
            assertThat(r.getId()).isEqualTo(9);
            assertThat(r.getMonto()).isEqualByComparingTo("500.00");
            assertThat(r.getTipoMovimiento()).isEqualTo("DEPOSITO");
            assertThat(r.getDescripcion()).isEqualTo("Deposito inicial");
        });
    }

    @Test
    void metodos_conEntradaNull_regresanNullSinFallar() {
        Cliente cliente = cliente();
        Domicilio domicilio = new Domicilio();
        domicilio.setCalle("Igual");

        mapper.actualizarEntity((ClienteActualizaRequest) null, cliente);
        mapper.actualizarEntity((DomicilioRequest) null, domicilio);

        assertThat(mapper.toEntity((ClienteRequest) null)).isNull();
        assertThat(mapper.toEntity((DomicilioRequest) null)).isNull();
        assertThat(mapper.toResponse((Cliente) null)).isNull();
        assertThat(mapper.toResponse((Domicilio) null)).isNull();
        assertThat(mapper.toResponse((Cuenta) null)).isNull();
        assertThat(mapper.toResponse((Saldo) null)).isNull();
        assertThat(mapper.toResponseList(null)).isNull();
        assertThat(mapper.toSaldoResponseList(null)).isNull();
        assertThat(cliente.getNombre()).isEqualTo("Pedro");
        assertThat(domicilio.getCalle()).isEqualTo("Igual");
    }
}
