package com.lamar.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record PrecuentaDto(
        Integer idPedido,
        String estadoPedido,
        String tipoComprobante,
        LocalDate fecha,
        LocalTime hora,
        Integer numeroMesa,
        String zona,
        String clienteNombre,
        String clienteDni,
        String atendidoPor,
        List<Linea> lineas,
        BigDecimal opGravada,
        BigDecimal igv,
        BigDecimal totalComprobante,
        BigDecimal propina,
        BigDecimal totalAPagar,
        String sonLetras) {

    public record Linea(
            String descripcion,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal total) {}
}