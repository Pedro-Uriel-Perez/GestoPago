package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.compras.BitacoraGestoPago;
import com.proyecto.servicios.model.gestopago.GestoPagoSenTxResponse;
import com.proyecto.servicios.repositorys.compras.BitacoraGestoPagoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

/**
 * Guarda en la tabla gestopago_bitacora cada peticion a GestoPago y lo que
 * respondio (o el error), como pide la especificacion ("Log evidence").
 *
 * REQUIRES_NEW: el registro se confirma aunque la operacion que lo rodea
 * termine en rollback, que es justo cuando mas sirve para aclaraciones. Si
 * guardar la bitacora falla, solo se avisa en el log: no debe tumbar el pago.
 */
@Component
@Slf4j
public class EvidenciaGestoPago {

    private final BitacoraGestoPagoRepository repository;
    private final ObjectMapper objectMapper;
    private final EvidenciaGestoPago self;

    public EvidenciaGestoPago(BitacoraGestoPagoRepository repository, ObjectMapper objectMapper,
                              @Lazy EvidenciaGestoPago self) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.self = self;
    }

    /**
     * Ejecuta la llamada a GestoPago y deja su evidencia. Si la llamada lanza
     * una excepcion, se registra y se vuelve a lanzar tal cual.
     */
    public GestoPagoSenTxResponse registrar(String operacion, String upc, String peticionEnClaro,
                                            Supplier<GestoPagoSenTxResponse> llamada) {
        long inicio = System.currentTimeMillis();
        try {
            GestoPagoSenTxResponse respuesta = llamada.get();
            self.guardar(operacion, upc, peticionEnClaro, respuesta, null, System.currentTimeMillis() - inicio);
            return respuesta;
        } catch (RuntimeException e) {
            self.guardar(operacion, upc, peticionEnClaro, null, e.getClass().getSimpleName(),
                    System.currentTimeMillis() - inicio);
            throw e;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardar(String operacion, String upc, String peticion, GestoPagoSenTxResponse respuesta,
                        String error, long duracionMs) {
        try {
            BitacoraGestoPago registro = new BitacoraGestoPago();
            registro.setOperacion(operacion);
            registro.setUpc(upc);
            registro.setPeticion(peticion);
            registro.setRespuesta(respuesta == null ? null : objectMapper.writeValueAsString(respuesta));
            registro.setCodigo(respuesta != null && respuesta.getMensaje() != null
                    ? respuesta.getMensaje().getCodigo() : null);
            registro.setError(error);
            registro.setDuracionMs(duracionMs);
            repository.save(registro);
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("No se pudo guardar la bitacora de GestoPago. operacion={}, upc={}", operacion, upc, e);
        }
    }
}
