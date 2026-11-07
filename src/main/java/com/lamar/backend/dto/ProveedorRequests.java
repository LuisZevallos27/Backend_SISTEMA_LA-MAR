package com.lamar.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ProveedorRequests {

    private ProveedorRequests() {}

    public record Crear(
            @NotBlank @Pattern(regexp = "\\d{11}", message = "El RUC debe tener 11 digitos") String ruc,
            @NotBlank @Size(max = 100) String razonSocial,
            @Size(max = 15) String telefono) {}

    public record Actualizar(
            @NotBlank @Size(max = 100) String razonSocial,
            @Size(max = 15) String telefono) {}
}