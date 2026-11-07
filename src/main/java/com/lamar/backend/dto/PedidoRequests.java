package com.lamar.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class PedidoRequests {

    private PedidoRequests() {}

    public record Abrir(@NotNull Integer idMesa, String dniCliente) {}

    public record AgregarItem(@NotNull Integer idPlato, @NotNull @Min(1) Integer cantidad) {}

    public record CambiarCantidad(@NotNull @Min(1) Integer cantidad) {}

    public record AsignarCliente(@NotBlank String dni) {}

    public record Cerrar(String tipoComprobante) {}
}