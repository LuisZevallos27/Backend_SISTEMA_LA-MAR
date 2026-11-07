package com.lamar.backend.repository;

import com.lamar.backend.entity.Insumo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsumoRepository extends JpaRepository<Insumo, Integer> {
    List<Insumo> findAllByOrderByNombreAsc();
    List<Insumo> findByNombreContainingIgnoreCaseOrderByNombreAsc(String texto);
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Integer id);
}