package com.lamar.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ClienteRequests {

    private ClienteRequests() {}

    public record Crear(
            @NotBlank @Pattern(regexp = "\\d{8}|\\d{11}", message = "Debe ser un DNI (8 digitos) o RUC (11)") String dni,
            @NotBlank @Size(max = 60) String nombres,
            @NotBlank @Size(max = 60) String apellidos,
            @Size(max = 15) String telefono,
            @Email @Size(max = 80) String correo) {}

    public record Actualizar(
            @NotBlank @Size(max = 60) String nombres,
            @NotBlank @Size(max = 60) String apellidos,
            @Size(max = 15) String telefono,
            @Email @Size(max = 80) String correo) {}
}