package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.compras.BitacoraGestoPago;
import com.proyecto.servicios.model.gestopago.GestoPagoSenTxResponse;
import com.proyecto.servicios.model.productlist.MensajeExternoDTO;
import com.proyecto.servicios.repositorys.compras.BitacoraGestoPagoRepository;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvidenciaGestoPagoTest {

    @Mock
    private BitacoraGestoPagoRepository repository;

    private EvidenciaGestoPago bitacora;

    @BeforeEach
    void setUp() {
        bitacora = new EvidenciaGestoPago(repository, new ObjectMapper(), null);
        // Sin Spring no hay proxy: se apunta a si misma para que "self" funcione.
        ReflectionTestUtils.setField(bitacora, "self", bitacora);
    }

    private BitacoraGestoPago registroGuardado() {
        ArgumentCaptor<BitacoraGestoPago> registro = ArgumentCaptor.forClass(BitacoraGestoPago.class);
        verify(repository).save(registro.capture());
        return registro.getValue();
    }

    @Test
    void registrar_guardaPeticionRespuestaYCodigoYRegresaLaRespuesta() {
        MensajeExternoDTO mensaje = new MensajeExternoDTO();
        mensaje.setCodigo("01");
        mensaje.setTexto("Operacion realizada con exito");
        GestoPagoSenTxResponse respuesta = new GestoPagoSenTxResponse();
        respuesta.setMensaje(mensaje);
        respuesta.setIdTx("946033922");

        GestoPagoSenTxResponse resultado = bitacora.registrar("sendTx", "UPC1", "{\"idProducto\":582}", () -> respuesta);

        assertThat(resultado).isSameAs(respuesta);
        BitacoraGestoPago registro = registroGuardado();
        assertThat(registro.getOperacion()).isEqualTo("sendTx");
        assertThat(registro.getUpc()).isEqualTo("UPC1");
        assertThat(registro.getPeticion()).isEqualTo("{\"idProducto\":582}");
        assertThat(registro.getRespuesta()).contains("946033922").contains("Operacion realizada con exito");
        assertThat(registro.getCodigo()).isEqualTo("01");
        assertThat(registro.getError()).isNull();
        assertThat(registro.getDuracionMs()).isNotNegative();
    }

    @Test
    void registrar_siLaLlamadaFalla_guardaElErrorYLoVuelveALanzar() {
        RetryableException timeout = mock(RetryableException.class);

        assertThatThrownBy(() -> bitacora.registrar("sendTx", "UPC1", "{}", () -> {
            throw timeout;
        })).isSameAs(timeout);

        BitacoraGestoPago registro = registroGuardado();
        assertThat(registro.getRespuesta()).isNull();
        assertThat(registro.getCodigo()).isNull();
        assertThat(registro.getError()).isNotBlank();
    }

    @Test
    void guardar_siFallaLaBaseDeDatos_noTumbaLaOperacion() {
        when(repository.save(any())).thenThrow(new IllegalStateException("sin conexion"));

        assertThatCode(() -> bitacora.guardar("sendTx", "UPC1", "{}", new GestoPagoSenTxResponse(), null, 5))
                .doesNotThrowAnyException();
    }
}
