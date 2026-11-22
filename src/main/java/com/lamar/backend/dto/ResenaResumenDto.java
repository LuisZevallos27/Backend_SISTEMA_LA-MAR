package com.lamar.backend.dto;

import java.util.List;

/**
 * Estadísticas de las reseñas de los últimos {@code dias} días (panel del
 * Administrador).
 */
public record ResenaResumenDto(
        int dias,
        long total,
        double promedioGeneral,
        List<ConteoEstrellas> distribucion,
        List<PlatoCalificado> mejoresPlatos,
        List<PlatoCalificado> peoresPlatos,
        List<CategoriaCalificada> porCategoria) {

    public record ConteoEstrellas(int estrellas, long cantidad) {
    }

    public record PlatoCalificado(Integer idPlato, String plato, String categoria, long resenas, double promedio) {
    }

    public record CategoriaCalificada(String categoria, long resenas, double promedio) {
    }
}