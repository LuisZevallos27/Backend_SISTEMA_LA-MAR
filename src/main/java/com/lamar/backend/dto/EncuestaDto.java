package com.lamar.backend.dto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import com.lamar.backend.documento.Encuesta;

public record EncuestaDto(
        String id,
        Integer idPedido,
        Integer numeroMesa,
        Integer idEmpleado,
        String mozo,
        LocalDateTime fecha,
        CalificacionesDto calificaciones,
        Boolean recomendaria,
        String comentario,
        List<String> etiquetas) {

    public record CalificacionesDto(Integer comida, Integer servicio, Integer ambiente, Integer tiempoEspera) {
    }

    public static EncuestaDto from(Encuesta e) {
        Encuesta.Calificaciones c = e.getCalificaciones();
        return new EncuestaDto(
                e.getId(),
                e.getIdPedido(),
                e.getNumeroMesa(),
                e.getIdEmpleado(),
                e.getMozo(),
                // El documento guarda el instante (UTC); la API responde en hora de Lima,
                // igual que el resto (pedidos, reservas).
                e.getFecha() == null ? null : LocalDateTime.ofInstant(e.getFecha(), ZoneId.systemDefault()),
                c == null ? null
                        : new CalificacionesDto(c.getComida(), c.getServicio(), c.getAmbiente(), c.getTiempoEspera()),
                e.getRecomendaria(),
                e.getComentario(),
                e.getEtiquetas());
    }
}