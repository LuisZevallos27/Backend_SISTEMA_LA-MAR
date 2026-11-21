package com.lamar.backend.documento;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

/**
 * Encuesta de satisfacción de un pedido. Colección "encuestas" en
 * LaMarFeedback.
 */
@Document(collection = "encuestas")
@Getter
@Setter
public class Encuesta {

    @Id
    private String id;

    // Referencias a SQL Server (MongoDB no tiene llaves foráneas)
    private Integer idPedido;
    private Integer numeroMesa;
    private Integer idEmpleado;
    private String mozo;

    /** Instante exacto (UTC), igual que lo guarda MongoDB. */
    private Instant fecha;

    private Calificaciones calificaciones;
    private Boolean recomendaria;
    private String comentario;
    private List<String> etiquetas = new ArrayList<>();

    @Getter
    @Setter
    public static class Calificaciones {
        private Integer comida;
        private Integer servicio;
        private Integer ambiente;
        private Integer tiempoEspera;
    }
}