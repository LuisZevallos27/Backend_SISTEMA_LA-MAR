package com.lamar.backend.service;

import com.lamar.backend.dto.ReservaDto;
import com.lamar.backend.dto.ReservaRequests;
import com.lamar.backend.entity.Cliente;
import com.lamar.backend.entity.Mesa;
import com.lamar.backend.entity.Reserva;
import com.lamar.backend.repository.ClienteRepository;
import com.lamar.backend.repository.MesaRepository;
import com.lamar.backend.repository.ReservaRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private static final List<String> ACTIVAS = List.of("Pendiente", "Confirmada");
    /** Tiempo que se considera ocupada una mesa por reserva. */
    private static final long DURACION_MINUTOS = 120;

    private final ReservaRepository reservaRepository;
    private final ClienteRepository clienteRepository;
    private final MesaRepository mesaRepository;

    @Transactional(readOnly = true)
    public List<ReservaDto> listar(LocalDate fecha, String estado) {
        LocalDate f = (fecha != null) ? fecha : LocalDate.now();
        return reservaRepository.findByFechaOrderByHoraAscMesaNumeroAsc(f).stream()
                .filter(r -> estado == null || estado.isBlank() || r.getEstado().equalsIgnoreCase(estado.trim()))
                .map(ReservaDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReservaDto obtener(Integer id) {
        return ReservaDto.from(buscar(id));
    }

    @Transactional
    public ReservaDto crear(ReservaRequests.Crear req) {
        Cliente cliente = clienteRepository.findByDni(req.dniCliente().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no registrado"));
        Mesa mesa = mesaRepository.findById(req.idMesa())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mesa no encontrada"));

        LocalDate hoy = LocalDate.now();
        if (req.fecha().isBefore(hoy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha no puede ser anterior a hoy");
        }
        if (req.fecha().equals(hoy) && req.hora().isBefore(LocalTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La hora de la reserva ya paso");
        }
        if (req.cantPersonas() > mesa.getCapacidad()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La mesa tiene capacidad para " + mesa.getCapacidad() + " personas");
        }

        boolean choca = reservaRepository
                .findByMesaIdAndFechaAndEstadoIn(mesa.getId(), req.fecha(), ACTIVAS).stream()
                .filter(o -> o.getHora() != null)
                .anyMatch(o -> Math.abs(Duration.between(o.getHora(), req.hora()).toMinutes()) < DURACION_MINUTOS);
        if (choca) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La mesa ya tiene una reserva cercana a esa hora (separacion minima: 2 horas)");
        }

        Reserva r = new Reserva();
        r.setCliente(cliente);
        r.setMesa(mesa);
        r.setCanal((req.canal() == null || req.canal().isBlank()) ? "Presencial" : req.canal().trim());
        r.setFecha(req.fecha());
        r.setHora(req.hora());
        r.setCantidadPersonas(req.cantPersonas());
        r.setEstado("Pendiente");
        reservaRepository.save(r);

        if (req.fecha().equals(hoy) && "Disponible".equals(mesa.getEstado())) {
            mesa.setEstado("Reservado");
        }
        return ReservaDto.from(r);
    }

    @Transactional
    public ReservaDto confirmar(Integer id) {
        Reserva r = buscar(id);
        if (!"Pendiente".equals(r.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede confirmar una reserva pendiente");
        }
        r.setEstado("Confirmada");
        return ReservaDto.from(r);
    }

    @Transactional
    public ReservaDto cancelar(Integer id) {
        Reserva r = activa(id);
        r.setEstado("Cancelada");

        Mesa mesa = r.getMesa();
        if (LocalDate.now().equals(r.getFecha()) && "Reservado".equals(mesa.getEstado())) {
            boolean quedaOtraHoy = reservaRepository
                    .findByMesaIdAndFechaAndEstadoIn(mesa.getId(), r.getFecha(), ACTIVAS).stream()
                    .anyMatch(o -> !o.getId().equals(r.getId()));
            if (!quedaOtraHoy) {
                mesa.setEstado("Disponible");
            }
        }
        return ReservaDto.from(r);
    }

    @Transactional
    public ReservaDto atender(Integer id) {
        Reserva r = activa(id);
        r.setEstado("Atendida");
        return ReservaDto.from(r);
    }

    private Reserva buscar(Integer id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
    }

    private Reserva activa(Integer id) {
        Reserva r = buscar(id);
        if (!ACTIVAS.contains(r.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La reserva ya esta " + r.getEstado());
        }
        return r;
    }
}