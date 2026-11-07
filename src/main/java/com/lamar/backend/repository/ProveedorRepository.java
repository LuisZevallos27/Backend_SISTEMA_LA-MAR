package com.lamar.backend.repository;

import com.lamar.backend.entity.Proveedor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {
    List<Proveedor> findAllByOrderByRazonSocialAsc();
    boolean existsByRuc(String ruc);
}