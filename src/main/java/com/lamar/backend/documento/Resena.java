package com.lamar.backend.documento;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

/**
 * Reseña de un plato dentro de un pedido. Colección "resenas" en LaMarFeedback.
 */
@Document(collection = "resenas")
@Getter
@Setter
public class Resena {

    @Id
    private String id;

    // Referencias a SQL Server (MongoDB no tiene llaves foráneas)
    private Integer idPedido;
    private Integer idPlato;

    // Copia del plato al momento de la reseña: sigue legible aunque el plato cambie
    // de nombre
    private String nombrePlato;
    private String categoria;

    private Integer estrellas;
    private String comentario;

    /** Instante exacto (UTC), igual que lo guarda MongoDB. */
    private Instant fecha;
}