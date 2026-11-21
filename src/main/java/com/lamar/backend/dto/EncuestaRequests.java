package com.lamar.backend.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class EncuestaRequests {

    private EncuestaRequests() {
    }

    public record Calificaciones(
            @NotNull @Min(1) @Max(5) Integer comida,
            @NotNull @Min(1) @Max(5) Integer servicio,
            @NotNull @Min(1) @Max(5) Integer ambiente,
            @NotNull @Min(1) @Max(5) Integer tiempoEspera) {
    }

    public record Crear(
            @NotNull Integer idPedido,
            @NotNull @Valid Calificaciones calificaciones,
            @NotNull Boolean recomendaria,
            @Size(max = 500) String comentario,
            @Size(max = 10) List<@NotBlank @Size(max = 30) String> etiquetas) {
    }
}