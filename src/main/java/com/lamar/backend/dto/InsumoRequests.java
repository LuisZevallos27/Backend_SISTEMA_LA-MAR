package com.lamar.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class InsumoRequests {

    private InsumoRequests() {}

    public record Guardar(
            @NotBlank @Size(max = 80) String nombre,
            @NotBlank @Size(max = 15) String unidadMedida) {}
}