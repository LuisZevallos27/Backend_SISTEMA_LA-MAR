package com.lamar.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Cliente")
@Getter @Setter
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer id;

    @Column(name = "dni_cliente", nullable = false, length = 11)
    private String dni;

    @Column(name = "nombres_cliente", nullable = false, length = 60)
    private String nombres;

    @Column(name = "apellidos_cliente", nullable = false, length = 60)
    private String apellidos;

    @Column(name = "telefono_cliente", length = 15)
    private String telefono;

    @Column(name = "correo_cliente", length = 80)
    private String correo;
}