package com.lamar.backend.repository;

import com.lamar.backend.entity.Cliente;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
    Optional<Cliente> findByDni(String dni);
    boolean existsByDni(String dni);
}