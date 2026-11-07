package com.lamar.backend.dto;

import com.lamar.backend.entity.Reserva;
import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaDto(
        Integer id,
        Integer idMesa,
        Integer numeroMesa,
        String zona,
        String clienteDni,
        String clienteNombre,
        String canal,
        LocalDate fecha,
        LocalTime hora,
        Integer cantPersonas,
        String estado) {

    public static ReservaDto from(Reserva r) {
        return new ReservaDto(
                r.getId(),
                r.getMesa().getId(),
                r.getMesa().getNumero(),
                r.getMesa().getZona(),
                r.getCliente().getDni(),
                r.getCliente().getNombres() + " " + r.getCliente().getApellidos(),
                r.getCanal(),
                r.getFecha(),
                r.getHora(),
                r.getCantidadPersonas(),
                r.getEstado());
    }
}