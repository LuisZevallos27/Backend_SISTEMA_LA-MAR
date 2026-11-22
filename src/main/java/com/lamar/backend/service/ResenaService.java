package com.lamar.backend.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.lamar.backend.documento.Resena;
import com.lamar.backend.dto.ResenaDto;
import com.lamar.backend.dto.ResenaRequests;
import com.lamar.backend.entity.DetallePedido;
import com.lamar.backend.entity.Pedido;
import com.lamar.backend.entity.Plato;
import com.lamar.backend.repository.PedidoRepository;
import com.lamar.backend.repository.ResenaRepository;
import com.mongodb.MongoException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResenaService {

    private final ResenaRepository resenaRepository;
    private final PedidoRepository pedidoRepository;

    public List<ResenaDto> listarPorPedido(Integer idPedido) {
        return enMongo(() -> resenaRepository.findByIdPedido(idPedido).stream()
                .map(ResenaDto::from)
                .toList());
    }

    /**
     * Últimas reseñas de un plato, de la más reciente a la más antigua (máximo 50).
     */
    public List<ResenaDto> listarPorPlato(Integer idPlato, int limite) {
        int tamano = Math.max(1, Math.min(limite, 50));
        return enMongo(() -> resenaRepository
                .findByIdPlato(idPlato, PageRequest.of(0, tamano, Sort.by(Sort.Direction.DESC, "fecha")))
                .stream()
                .map(ResenaDto::from)
                .toList());
    }

    public List<ResenaDto> crear(ResenaRequests.Crear req) {
        // 1. Validaciones contra SQL Server (fuente de verdad de los pedidos y los
        // platos)
        Pedido pedido = pedidoRepository.buscarConPlatos(req.idPedido())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        if (!"Cerrado".equals(pedido.getEstadoPedido())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Solo se puede reseñar un pedido cerrado");
        }

        Map<Integer, Plato> platosDelPedido = pedido.getDetalles().stream()
                .map(DetallePedido::getPlato)
                .collect(Collectors.toMap(Plato::getId, p -> p, (a, b) -> a));

        Set<Integer> enLaSolicitud = new HashSet<>();
        for (ResenaRequests.Item item : req.resenas()) {
            if (!platosDelPedido.containsKey(item.idPlato())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El plato " + item.idPlato() + " no pertenece al pedido");
            }
            if (!enLaSolicitud.add(item.idPlato())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El plato " + item.idPlato() + " está repetido en la solicitud");
            }
        }

        // 2. Armar los documentos: nombre y categoría salen de SQL Server, no del
        // cliente
        Instant ahora = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        List<Resena> documentos = req.resenas().stream().map(item -> {
            Plato plato = platosDelPedido.get(item.idPlato());
            Resena r = new Resena();
            r.setIdPedido(pedido.getId());
            r.setIdPlato(plato.getId());
            r.setNombrePlato(plato.getNombre());
            r.setCategoria(plato.getCategoria());
            r.setEstrellas(item.estrellas());
            r.setComentario(item.comentario() == null || item.comentario().isBlank()
                    ? null
                    : item.comentario().trim());
            r.setFecha(ahora);
            return r;
        }).toList();

        // 3. Guardar en MongoDB. Si Mongo falla, solo se pierden las reseñas: el resto
        // del sistema sigue.
        try {
            Set<Integer> yaResenados = resenaRepository.findByIdPedido(pedido.getId()).stream()
                    .map(Resena::getIdPlato)
                    .collect(Collectors.toSet());
            if (documentos.stream().anyMatch(d -> yaResenados.contains(d.getIdPlato()))) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Alguno de los platos ya tiene reseña en este pedido");
            }
            return resenaRepository.saveAll(documentos).stream().map(ResenaDto::from).toList();
        } catch (DuplicateKeyException ex) {
            // Dos envíos simultáneos: el índice único UQ_resena_pedido_plato lo detiene
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Alguno de los platos ya tiene reseña en este pedido");
        } catch (DataAccessException | MongoException ex) {
            throw noDisponible();
        }
    }

    private <T> T enMongo(Supplier<T> accion) {
        try {
            return accion.get();
        } catch (DataAccessException | MongoException ex) {
            throw noDisponible();
        }
    }

    private ResponseStatusException noDisponible() {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "Las reseñas no están disponibles por el momento");
    }
}