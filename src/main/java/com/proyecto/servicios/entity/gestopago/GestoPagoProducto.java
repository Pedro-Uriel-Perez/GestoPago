package com.proyecto.servicios.entity.gestopago;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Copia local (cache) de un producto/servicio del catalogo de GestoPago.
 * Se sincroniza una vez al dia (ver GestoPagoProductListSyncServiceImpl)
 * porque el proveedor solo permite consultar getProductList.do hasta 3
 * veces por dia y prohibe usarlo como fuente directa para el frontend.
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
