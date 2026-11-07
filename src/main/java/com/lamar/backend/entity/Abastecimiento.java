package com.lamar.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Abastecimiento")
@Getter @Setter
public class Abastecimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_abastecimiento")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proveedor")
    private Proveedor proveedor;

    @Column(name = "fecha")
    private LocalDateTime fecha;

    @Column(nullable = false, length = 20)
    private String estado;

    @OneToMany(mappedBy = "abastecimiento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleAbastecimiento> detalles = new ArrayList<>();
}