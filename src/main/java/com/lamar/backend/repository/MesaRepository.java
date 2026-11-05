package com.lamar.backend.repository;

import com.lamar.backend.entity.Mesa;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MesaRepository extends JpaRepository<Mesa, Integer> {
    List<Mesa> findAllByOrderByNumero();

    List<Mesa> findByZonaOrderByNumero(String zona);
}