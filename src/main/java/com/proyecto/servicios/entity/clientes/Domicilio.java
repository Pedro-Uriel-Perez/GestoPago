package com.proyecto.servicios.entity.clientes;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "domicilios")
@Getter
@Setter
public class Domicilio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    private Cliente cliente;

    @Column(name = "calle", nullable = false, columnDefinition = "TEXT")
    private String calle;

    @Column(name = "numero_exterior", nullable = false, columnDefinition = "TEXT")
    private String numeroExterior;

    @Column(name = "numero_interior", columnDefinition = "TEXT")
    private String numeroInterior;

    @Column(name = "colonia", nullable = false, columnDefinition = "TEXT")
    private String colonia;

    @Column(name = "municipio", nullable = false, columnDefinition = "TEXT")
    private String municipio;

    @Column(name = "estado", nullable = false, columnDefinition = "TEXT")
    private String estado;

    @Column(name = "codigo_postal", nullable = false, columnDefinition = "TEXT")
    private String codigoPostal;

    @Column(name = "pais", nullable = false, columnDefinition = "TEXT")
    private String pais;
}
