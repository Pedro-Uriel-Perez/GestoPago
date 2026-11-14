package com.proyecto.servicios.entity.compras;

import com.proyecto.servicios.config.ZonaHoraria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro de una operacion de pago contra GestoPago, para el historial del
 * cliente. Se guarda una fila por operacion confirmada con PIN, sea cual sea
 * el resultado: EXITOSO, RECHAZADO o PENDIENTE (resultado no confirmado).
 */
@Entity
@Table(name = "pagos")
@Getter
@Setter
public class Pago {

    public static final String ESTADO_EXITOSO = "EXITOSO";
    public static final String ESTADO_RECHAZADO = "RECHAZADO";
    public static final String ESTADO_PENDIENTE = "PENDIENTE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cliente_id", nullable = false)
    private Integer clienteId;

    @Column(name = "cuenta_id", nullable = false)
    private Integer cuentaId;

    @Column(name = "operacion_id", nullable = false, unique = true, length = 36)
    private String operacionId;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "id_servicio")
    private Integer idServicio;

    @Column(name = "nombre_producto", columnDefinition = "TEXT")
    private String nombreProducto;

    @Column(name = "nombre_servicio", columnDefinition = "TEXT")
    private String nombreServicio;

    @Column(name = "telefono", columnDefinition = "TEXT")
    private String telefono;

    @Column(name = "referencia", columnDefinition = "TEXT")
    private String referencia;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "estado", nullable = false, length = 10)
    private String estado;

    @Column(name = "id_transaccion", columnDefinition = "TEXT")
    private String idTransaccion;

    @Column(name = "numero_autorizacion", columnDefinition = "TEXT")
    private String numeroAutorizacion;

    @Column(name = "codigo_respuesta", columnDefinition = "TEXT")
    private String codigoRespuesta;

    @Column(name = "mensaje", columnDefinition = "TEXT")
    private String mensaje;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    @PrePersist
    void alCrear() {
        fecha = LocalDateTime.now(ZonaHoraria.MEXICO);
    }
}
