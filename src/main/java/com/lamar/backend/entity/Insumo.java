package com.lamar.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Insumo")
@Getter @Setter
public class Insumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_insumo")
    private Integer id;

    @Column(name = "nombre_insumo", nullable = false, length = 80)
    private String nombre;

    @Column(name = "unidad_medida", nullable = false, length = 15)
    private String unidadMedida;
}