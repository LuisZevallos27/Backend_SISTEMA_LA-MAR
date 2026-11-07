package com.lamar.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoDto(
        Integer id,
        Integer idMesa,
        Integer numeroMesa,
        String zona,
        String estado,
        LocalDateTime fechaHora,
        String tipoComprobante,
        String empleado,
        String clienteDni,
        String cliente,
        List<Item> items,
        BigDecimal baseImponible,
        BigDecimal igv,
        BigDecimal total) {

    public record Item(
            Integer idPlato,
            String nombre,
            BigDecimal precioUnitario,
            Integer cantidad,
            BigDecimal subtotal) {}
}