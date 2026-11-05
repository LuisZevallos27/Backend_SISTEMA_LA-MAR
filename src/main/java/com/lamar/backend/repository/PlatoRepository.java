package com.lamar.backend.repository;

import com.lamar.backend.entity.Plato;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatoRepository extends JpaRepository<Plato, Integer> {
    List<Plato> findByCategoria(String categoria);
    List<Plato> findByNombreContainingIgnoreCase(String texto);
}
