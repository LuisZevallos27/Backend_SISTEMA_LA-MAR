package com.lamar.backend.dto;

import com.lamar.backend.entity.Plato;
import java.math.BigDecimal;

public record PlatoDto(
        Integer id,
        String nombre,
        String categoria,
        BigDecimal precio,
        String descripcion,
        String imagenUrl) {

    public static PlatoDto from(Plato p) {
        return new PlatoDto(p.getId(), p.getNombre(), p.getCategoria(),
                p.getPrecio(), p.getDescripcion(), p.getImagenUrl());
    }
}
