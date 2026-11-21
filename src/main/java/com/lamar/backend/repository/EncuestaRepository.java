package com.lamar.backend.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.lamar.backend.documento.Encuesta;

public interface EncuestaRepository extends MongoRepository<Encuesta, String> {

    Optional<Encuesta> findByIdPedido(Integer idPedido);

    boolean existsByIdPedido(Integer idPedido);
}