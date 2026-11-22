package com.lamar.backend.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ResenaRequests {

    private ResenaRequests() {
    }

    /** Reseña de un plato. */
    public record Item(
            @NotNull Integer idPlato,
            @NotNull @Min(1) @Max(5) Integer estrellas,
            @Size(max = 500) String comentario) {
    }

    /** Reseñas de varios platos de un mismo pedido, enviadas juntas. */
    public record Crear(
            @NotNull Integer idPedido,
            @NotEmpty @Size(max = 20) @Valid List<Item> resenas) {
    }
}