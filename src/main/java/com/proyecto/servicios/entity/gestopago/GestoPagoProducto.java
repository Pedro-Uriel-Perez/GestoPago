package com.proyecto.servicios.entity.gestopago;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Copia local en Postgres del catalogo de productos/servicios de GestoPago
 * (fuente de verdad/respaldo). Redis (cache "productos") acelera las
 * lecturas encima de esta tabla; ver GestoPagoProductListSyncServiceImpl
 * (escribe aqui una vez al dia) y ProductListServiceImpl (lee de aqui en
 * cache-miss).
 */
@Entity
@Table(name = "gestopago_productos")
@Getter
@Setter
public class GestoPagoProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "id_servicio")
    private Integer idServicio;

    @Column(name = "servicio", columnDefinition = "TEXT")
    private String servicio;

    @Column(name = "producto", columnDefinition = "TEXT")
    private String producto;

    @Column(name = "id_cat_tipo_servicio")
    private Integer idCatTipoServicio;

    @Column(name = "tipo_front")
    private Integer tipoFront;

    @Column(name = "has_digito_verificador")
    private Boolean hasDigitoVerificador;

    @Column(name = "tipo_referencia", columnDefinition = "TEXT")
    private String tipoReferencia;

    @Column(name = "precio", columnDefinition = "TEXT")
    private String precio;

    @Column(name = "legend", columnDefinition = "TEXT")
    private String legend;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    @PreUpdate
    void onSave() {
        fechaActualizacion = LocalDateTime.now();
    }
}
