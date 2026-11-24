package com.lamar.backend.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.lamar.backend.entity.Reserva;

/**
 * Decide qué reserva de hoy corresponde a un pedido que se abre ahora en una
 * mesa.
 *
 * Un pedido corresponde a una reserva si se abre entre 30 minutos antes y 2
 * horas después de su hora.
 * Las reservas activas (Pendiente o Confirmada) tienen prioridad. Si no hay
 * ninguna, se acepta una que
 * Recepción ya marcó "Atendida" a mano, pero solo si ningún pedido la usó
 * todavía: si no, un cliente
 * sin reserva que llega después recibiría el cliente de una reserva que ya se
 * consumió.
 */
final class ReservaDelPedido {

    /**
     * El pedido puede abrirse hasta 30 minutos antes de la hora de la reserva...
     */
    static final long MINUTOS_ANTES = 30;
    /** ...y hasta 2 horas después. */
    static final long MINUTOS_DESPUES = 120;

    private ReservaDelPedido() {
        
    }

    /**
     * @param activas   reservas de hoy en esa mesa que siguen Pendientes o
     *                  Confirmadas
     * @param atendidas reservas de hoy en esa mesa ya marcadas "Atendida"
     * @param ahora     hora actual
     * @param yaUsada   indica si algún pedido ya usó esa reserva (solo se consulta
     *                  para las atendidas)
     */
    static Optional<Reserva> elegir(List<Reserva> activas, List<Reserva> atendidas, LocalTime ahora,
            Predicate<Reserva> yaUsada) {
        Optional<Reserva> activa = masTemprana(activas, ahora, r -> true);
        if (activa.isPresent()) {
            return activa;
        }
        return masTemprana(atendidas, ahora, r -> !yaUsada.test(r));
    }

    /** Desde cuándo un pedido en la mesa cuenta como "uso" de esta reserva. */
    static LocalDateTime inicioDeLaVentana(LocalDate dia, Reserva reserva) {
        if (reserva.getHora() == null) {
            return dia.atStartOfDay();
        }
        return LocalDateTime.of(dia, reserva.getHora()).minusMinutes(MINUTOS_ANTES);
    }

    static boolean dentroDeLaVentana(Reserva reserva, LocalTime ahora) {
        if (reserva.getHora() == null) {
            return true;
        }
        // minutos hasta la reserva (negativo si ya pasó su hora)
        long minutos = Duration.between(ahora, reserva.getHora()).toMinutes();
        return minutos <= MINUTOS_ANTES && minutos >= -MINUTOS_DESPUES;
    }

    private static Optional<Reserva> masTemprana(List<Reserva> reservas, LocalTime ahora, Predicate<Reserva> filtro) {
        return reservas.stream()
                .filter(r -> dentroDeLaVentana(r, ahora))
                .filter(filtro)
                .min(Comparator.comparing(Reserva::getHora, Comparator.nullsFirst(Comparator.naturalOrder())));
    }
}