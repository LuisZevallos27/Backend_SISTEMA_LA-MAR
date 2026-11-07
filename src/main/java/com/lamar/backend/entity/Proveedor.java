package com.lamar.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "Proveedor")
@Getter @Setter
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor")
    private Integer id;

    // CHAR(11): sin esta anotacion la validacion del esquema falla
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ruc_proveedor", nullable = false, length = 11)
    private String ruc;

    @Column(name = "prov_razon_social", nullable = false, length = 100)
    private String razonSocial;

    @Column(name = "telefono_proveedor", length = 15)
    private String telefono;
}