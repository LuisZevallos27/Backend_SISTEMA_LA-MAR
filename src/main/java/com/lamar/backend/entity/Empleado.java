package com.lamar.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "Empleado")
@Getter
@Setter
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empleado")
    private Integer id;

    // La columna es CHAR(8); sin esta anotación Hibernate espera VARCHAR y la
    // validación falla
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "dni_empleado", nullable = false, length = 8)
    private String dni;

    @Column(name = "nombres_empleado", nullable = false, length = 60)
    private String nombres;

    @Column(name = "apellidos_empleado", nullable = false, length = 60)
    private String apellidos;

    @Column(nullable = false, length = 40)
    private String cargo;

    @Column(nullable = false, length = 20)
    private String turno;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;
}