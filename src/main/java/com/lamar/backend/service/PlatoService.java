package com.lamar.backend.service;

import com.lamar.backend.dto.PlatoDto;
import com.lamar.backend.entity.Plato;
import com.lamar.backend.repository.PlatoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PlatoService {

    private final PlatoRepository platoRepository;

    public List<PlatoDto> listar(String categoria, String buscar) {
        boolean hayCategoria = categoria != null && !categoria.isBlank();
        boolean hayTexto = buscar != null && !buscar.isBlank();

        List<Plato> platos;
        if (hayCategoria && hayTexto) {
            platos = platoRepository
                    .findByCategoriaIgnoreCaseAndNombreContainingIgnoreCaseOrderByNombreAsc(
                            categoria.trim(), buscar.trim());
        } else if (hayCategoria) {
            platos = platoRepository.findByCategoriaIgnoreCaseOrderByNombreAsc(categoria.trim());
        } else if (hayTexto) {
            platos = platoRepository
                    .findByNombreContainingIgnoreCaseOrderByCategoriaAscNombreAsc(buscar.trim());
        } else {
            platos = platoRepository.findAllByOrderByCategoriaAscNombreAsc();
        }
        return platos.stream().map(PlatoDto::from).toList();
    }

    public List<String> categorias() {
        return platoRepository.listarCategorias();
    }

    public PlatoDto obtener(Integer id) {
        return platoRepository.findById(id)
                .map(PlatoDto::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Plato no encontrado"));
    }
}