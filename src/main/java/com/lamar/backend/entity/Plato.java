package com.lamar.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Plato")
@Getter
@Setter
public class Plato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plato")
    private Integer id;

    @Column(name = "nombre_plato", nullable = false, length = 80)
    private String nombre;

    @Column(length = 40)
    private String categoria;

    @Column(name = "precio_plato", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(length = 200)
    private String descripcion;

    @Column(name = "imagen_url", length = 255)
    private String imagenUrl;
}