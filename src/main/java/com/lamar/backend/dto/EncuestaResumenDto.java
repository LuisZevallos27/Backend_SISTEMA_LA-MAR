package com.lamar.backend.dto;

import java.util.List;

/**
 * Estadísticas de las encuestas de los últimos {@code dias} días (panel del
 * Administrador).
 */
public record EncuestaResumenDto(
        int dias,
        long total,
        Promedios promedios,
        double porcentajeRecomienda,
        List<ConteoEstrellas> distribucionComida,
        List<RankingMozo> rankingMozos,
        List<EtiquetaFrecuente> etiquetas) {

    /**
     * Promedios de 1 a 5. "general" es el promedio de las cuatro calificaciones.
     */
    public record Promedios(double comida, double servicio, double ambiente, double tiempoEspera, double general) {
    }

    public record ConteoEstrellas(int estrellas, long cantidad) {
    }

    public record RankingMozo(Integer idEmpleado, String mozo, long encuestas, double servicio, double general) {
    }

    public record EtiquetaFrecuente(String etiqueta, long veces) {
    }
}