package com.lamar.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AbastecimientoDto(
        Integer id,
        Integer idProveedor,
        String proveedor,
        String ruc,
        LocalDateTime fecha,
        String estado,
        List<Item> items,
        BigDecimal costoTotal) {

    public record Item(
            Integer idInsumo,
            String insumo,
            String unidadMedida,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            BigDecimal costoTotal) {}
}