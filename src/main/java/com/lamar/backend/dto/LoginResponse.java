package com.lamar.backend.dto;

public record LoginResponse(
        String token,
        long expiraEnMinutos,
        Integer idEmpleado,
        String nombres,
        String apellidos,
        String cargo) {
}