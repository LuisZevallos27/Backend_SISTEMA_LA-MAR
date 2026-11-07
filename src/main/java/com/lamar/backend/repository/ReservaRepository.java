package com.lamar.backend.repository;

import com.lamar.backend.entity.Reserva;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    List<Reserva> findByFechaOrderByHoraAscMesaNumeroAsc(LocalDate fecha);

    List<Reserva> findByMesaIdAndFechaAndEstadoIn(Integer mesaId, LocalDate fecha, Collection<String> estados);

    Optional<Reserva> findFirstByMesaIdAndFechaAndEstadoInOrderByHoraAsc(
            Integer mesaId, LocalDate fecha, Collection<String> estados);
}