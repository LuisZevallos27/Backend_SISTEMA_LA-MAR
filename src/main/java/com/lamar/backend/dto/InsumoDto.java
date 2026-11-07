package com.lamar.backend.dto;

import java.math.BigDecimal;

public record InsumoDto(
        Integer id,
        String nombre,
        String unidadMedida,
        BigDecimal recibido,
        BigDecimal consumido,
        BigDecimal stock) {}