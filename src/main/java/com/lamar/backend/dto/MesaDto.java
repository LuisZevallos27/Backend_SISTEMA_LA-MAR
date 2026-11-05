package com.lamar.backend.dto;

import com.lamar.backend.entity.Mesa;

public record MesaDto(Integer id, Integer numero, Integer capacidad, String estado, String zona) {

    public static MesaDto from(Mesa m) {
        return new MesaDto(m.getId(), m.getNumero(), m.getCapacidad(), m.getEstado(), m.getZona());
    }
}