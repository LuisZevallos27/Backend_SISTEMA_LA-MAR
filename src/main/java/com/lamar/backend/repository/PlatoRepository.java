package com.lamar.backend.repository;

import com.lamar.backend.entity.Plato;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PlatoRepository extends JpaRepository<Plato, Integer> {

    List<Plato> findAllByOrderByCategoriaAscNombreAsc();

    List<Plato> findByCategoriaIgnoreCaseOrderByNombreAsc(String categoria);

    List<Plato> findByNombreContainingIgnoreCaseOrderByCategoriaAscNombreAsc(String texto);

    List<Plato> findByCategoriaIgnoreCaseAndNombreContainingIgnoreCaseOrderByNombreAsc(
            String categoria, String texto);

    @Query("select distinct p.categoria from Plato p where p.categoria is not null order by p.categoria")
    List<String> listarCategorias();
}