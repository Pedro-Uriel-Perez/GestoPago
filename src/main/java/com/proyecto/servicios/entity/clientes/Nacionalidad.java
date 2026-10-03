package com.proyecto.servicios.entity.clientes;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "nacionalidades")
@Getter
@Setter
public class Nacionalidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, unique = true, columnDefinition = "TEXT")
    private String nombre;
}
