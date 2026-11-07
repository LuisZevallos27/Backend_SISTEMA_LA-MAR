package com.lamar.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public final class ReservaRequests {

    private ReservaRequests() {}

    public record Crear(
            @NotBlank String dniCliente,
            @NotNull Integer idMesa,
            @NotNull LocalDate fecha,
            @NotNull LocalTime hora,
            @NotNull @Min(1) Integer cantPersonas,
            @Size(max = 20) String canal) {}
}