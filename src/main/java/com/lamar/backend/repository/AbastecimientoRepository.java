package com.lamar.backend.repository;

import com.lamar.backend.entity.Abastecimiento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AbastecimientoRepository extends JpaRepository<Abastecimiento, Integer> {
    List<Abastecimiento> findAllByOrderByFechaDesc();
    List<Abastecimiento> findByEstadoOrderByFechaDesc(String estado);
}