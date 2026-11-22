package com.lamar.backend.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.lamar.backend.documento.Resena;

public interface ResenaRepository extends MongoRepository<Resena, String> {

    List<Resena> findByIdPedido(Integer idPedido);

    List<Resena> findByIdPlato(Integer idPlato, Pageable pageable);
}