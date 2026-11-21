package com.lamar.backend.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.lamar.backend.documento.Encuesta;
import com.lamar.backend.dto.EncuestaDto;
import com.lamar.backend.dto.EncuestaRequests;
import com.lamar.backend.entity.Empleado;
import com.lamar.backend.entity.Pedido;
import com.lamar.backend.repository.EncuestaRepository;
import com.lamar.backend.repository.PedidoRepository;
import com.mongodb.MongoException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EncuestaService {

    private final EncuestaRepository encuestaRepository;
    private final PedidoRepository pedidoRepository;

    /** Últimas encuestas, de la más reciente a la más antigua (máximo 100). */
    public List<EncuestaDto> listar(int limite) {
        int tamano = Math.max(1, Math.min(limite, 100));
        return encuestaRepository
                .findAll(PageRequest.of(0, tamano, Sort.by(Sort.Direction.DESC, "fecha")))
                .map(EncuestaDto::from)
                .getContent();
    }

    public EncuestaDto obtenerPorPedido(Integer idPedido) {
        return encuestaRepository.findByIdPedido(idPedido)
                .map(EncuestaDto::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "El pedido no tiene encuesta"));
    }

    public EncuestaDto crear(EncuestaRequests.Crear req) {
        // 1. Validaciones contra SQL Server (fuente de verdad de los pedidos)
        Pedido pedido = pedidoRepository.buscarConMesaYEmpleado(req.idPedido())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        if (!"Cerrado".equals(pedido.getEstadoPedido())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Solo se puede encuestar un pedido cerrado");
        }

        // 2. Armar el documento: mesa, mozo y fecha salen del servidor, no del cliente
        Empleado empleado = pedido.getEmpleado();
        Encuesta e = new Encuesta();
        e.setIdPedido(pedido.getId());
        e.setNumeroMesa(pedido.getMesa().getNumero());
        e.setIdEmpleado(empleado.getId());
        e.setMozo(empleado.getNombres() + " " + empleado.getApellidos());
        e.setFecha(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));

        Encuesta.Calificaciones c = new Encuesta.Calificaciones();
        c.setComida(req.calificaciones().comida());
        c.setServicio(req.calificaciones().servicio());
        c.setAmbiente(req.calificaciones().ambiente());
        c.setTiempoEspera(req.calificaciones().tiempoEspera());
        e.setCalificaciones(c);

        e.setRecomendaria(req.recomendaria());
        e.setComentario(req.comentario() == null || req.comentario().isBlank()
                ? null
                : req.comentario().trim());
        if (req.etiquetas() != null) {
            e.setEtiquetas(new ArrayList<>(req.etiquetas().stream()
                    .map(t -> t.trim().toLowerCase(Locale.ROOT))
                    .distinct()
                    .toList()));
        }

        // 3. Guardar en MongoDB. Si Mongo falla, solo se pierde la encuesta: el resto
        // del sistema sigue.
        try {
            if (encuestaRepository.existsByIdPedido(pedido.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido ya tiene encuesta");
            }
            return EncuestaDto.from(encuestaRepository.save(e));
        } catch (DuplicateKeyException ex) {
            // Dos envíos simultáneos: el índice único UQ_encuesta_pedido lo detiene
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido ya tiene encuesta");
        } catch (DataAccessException | MongoException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Las encuestas no están disponibles por el momento");
        }
    }
}