package com.lamar.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Detalle_Abastecimiento")
@Getter @Setter
public class DetalleAbastecimiento {

    @EmbeddedId
    private DetalleAbastecimientoId id = new DetalleAbastecimientoId();

    @MapsId("idAbastecimiento")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_abastecimiento")
    private Abastecimiento abastecimiento;

    @MapsId("idInsumo")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_insumo")
    private Insumo insumo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "costo_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoTotal;
}