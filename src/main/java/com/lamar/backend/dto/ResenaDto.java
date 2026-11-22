package com.lamar.backend.dto;

import java.time.LocalDateTime;
import java.time.ZoneId;

import com.lamar.backend.documento.Resena;

public record ResenaDto(
        String id,
        Integer idPedido,
        Integer idPlato,
        String nombrePlato,
        String categoria,
        Integer estrellas,
        String comentario,
        LocalDateTime fecha) {

    public static ResenaDto from(Resena r) {
        return new ResenaDto(
                r.getId(),
                r.getIdPedido(),
                r.getIdPlato(),
                r.getNombrePlato(),
                r.getCategoria(),
                r.getEstrellas(),
                r.getComentario(),
                // El documento guarda el instante (UTC); la API responde en hora de Lima
                r.getFecha() == null ? null : LocalDateTime.ofInstant(r.getFecha(), ZoneId.systemDefault()));
    }
}