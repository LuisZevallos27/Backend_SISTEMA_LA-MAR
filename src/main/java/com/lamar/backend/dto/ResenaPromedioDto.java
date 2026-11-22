package com.lamar.backend.dto;

/**
 * Promedio de estrellas de un plato (todas sus reseñas). Sirve para mostrarlo
 * en la carta.
 */
public record ResenaPromedioDto(Integer idPlato, double promedio, long resenas) {

}