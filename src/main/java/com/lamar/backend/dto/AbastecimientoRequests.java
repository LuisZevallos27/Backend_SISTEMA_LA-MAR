package com.lamar.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public final class AbastecimientoRequests {

    private AbastecimientoRequests() {}

    public record Crear(
            @NotNull Integer idProveedor,
            @NotEmpty @Valid List<Item> items) {}

    public record Item(
            @NotNull Integer idInsumo,
            @NotNull @DecimalMin("0.01") BigDecimal cantidad,
            @NotNull @DecimalMin("0.00") BigDecimal costoTotal) {}
}