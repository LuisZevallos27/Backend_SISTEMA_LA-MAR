package com.lamar.backend.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.lamar.backend.dto.EncuestaResumenDto;
import com.lamar.backend.dto.EncuestaResumenDto.ConteoEstrellas;
import com.lamar.backend.dto.EncuestaResumenDto.EtiquetaFrecuente;
import com.lamar.backend.dto.EncuestaResumenDto.Promedios;
import com.lamar.backend.dto.EncuestaResumenDto.RankingMozo;
import com.mongodb.MongoException;

import lombok.RequiredArgsConstructor;

/**
 * Estadísticas con el framework de agregación de MongoDB.
 * Son las mismas consultas que se probaron en el script BD_LA_MAR_mongo.js.
 */
@Service
@RequiredArgsConstructor
public class EncuestaResumenService {

    private static final String COLECCION = "encuestas";

    /** Un mozo con menos encuestas que esto no entra al ranking: un solo voto no es representativo. */
    private static final int MIN_ENCUESTAS_RANKING = 3;
    private static final int MAX_ETIQUETAS = 8;

    private static final String GRUPO_GENERAL = """
            { "$group": { "_id": null, "total": { "$sum": 1 },
              "comida": { "$avg": "$calificaciones.comida" },
              "servicio": { "$avg": "$calificaciones.servicio" },
              "ambiente": { "$avg": "$calificaciones.ambiente" },
              "tiempoEspera": { "$avg": "$calificaciones.tiempoEspera" },
              "recomiendan": { "$sum": { "$cond": ["$recomendaria", 1, 0] } } } }
            """;

    private static final String GRUPO_MOZOS = """
            { "$group": { "_id": { "id": "$idEmpleado", "nombre": "$mozo" }, "encuestas": { "$sum": 1 },
              "comida": { "$avg": "$calificaciones.comida" },
              "servicio": { "$avg": "$calificaciones.servicio" },
              "ambiente": { "$avg": "$calificaciones.ambiente" },
              "tiempoEspera": { "$avg": "$calificaciones.tiempoEspera" } } }
            """;

    private static final String GRUPO_ESTRELLAS_COMIDA = """
            { "$group": { "_id": "$calificaciones.comida", "cantidad": { "$sum": 1 } } }
            """;

    private static final String DESGLOSAR_ETIQUETAS = """
            { "$unwind": "$etiquetas" }
            """;

    private static final String GRUPO_ETIQUETAS = """
            { "$group": { "_id": "$etiquetas", "veces": { "$sum": 1 } } }
            """;

    private static final String ORDEN_ETIQUETAS = """
            { "$sort": { "veces": -1, "_id": 1 } }
            """;

    private final MongoTemplate mongoTemplate;

    public EncuestaResumenDto resumen(int dias) {
        int periodo = Math.max(1, Math.min(dias, 365));
        Document filtro = new Document("$match", new Document("fecha",
                new Document("$gte", Date.from(Instant.now().minus(periodo, ChronoUnit.DAYS)))));

        try {
            // 1. Totales y promedios generales
            long total = 0;
            double porcentajeRecomienda = 0;
            Promedios promedios = new Promedios(0, 0, 0, 0, 0);
            List<Document> general = ejecutar(filtro, Document.parse(GRUPO_GENERAL));
            if (!general.isEmpty()) {
                Document g = general.get(0);
                total = ((Number) g.get("total")).longValue();
                promedios = promedios(g);
                porcentajeRecomienda = Math.round(numero(g, "recomiendan") * 1000.0 / total) / 10.0;
            }

            // 2. Distribución de estrellas de "comida" (siempre de 5 a 1, aunque no haya votos)
            Map<Integer, Long> conteo = new HashMap<>();
            for (Document d : ejecutar(filtro, Document.parse(GRUPO_ESTRELLAS_COMIDA))) {
                conteo.put(((Number) d.get("_id")).intValue(), ((Number) d.get("cantidad")).longValue());
            }
            List<ConteoEstrellas> distribucion = new ArrayList<>();
            for (int estrellas = 5; estrellas >= 1; estrellas--) {
                distribucion.add(new ConteoEstrellas(estrellas, conteo.getOrDefault(estrellas, 0L)));
            }

            // 3. Ranking de mozos (mejor promedio general primero)
            List<RankingMozo> ranking = ejecutar(filtro, Document.parse(GRUPO_MOZOS)).stream()
                    .filter(d -> numero(d, "encuestas") >= MIN_ENCUESTAS_RANKING)
                    .map(d -> {
                        Document id = d.get("_id", Document.class);
                        Promedios p = promedios(d);
                        return new RankingMozo(
                                ((Number) id.get("id")).intValue(),
                                id.getString("nombre"),
                                ((Number) d.get("encuestas")).longValue(),
                                p.servicio(),
                                p.general());
                    })
                    .sorted((a, b) -> {
                        int porPromedio = Double.compare(b.general(), a.general());
                        return porPromedio != 0 ? porPromedio : Long.compare(b.encuestas(), a.encuestas());
                    })
                    .toList();

            // 4. Etiquetas más frecuentes
            List<EtiquetaFrecuente> etiquetas = ejecutar(
                    filtro,
                    Document.parse(DESGLOSAR_ETIQUETAS),
                    Document.parse(GRUPO_ETIQUETAS),
                    Document.parse(ORDEN_ETIQUETAS),
                    new Document("$limit", MAX_ETIQUETAS)).stream()
                    .map(d -> new EtiquetaFrecuente(d.getString("_id"), ((Number) d.get("veces")).longValue()))
                    .toList();

            return new EncuestaResumenDto(
                    periodo, total, promedios, porcentajeRecomienda, distribucion, ranking, etiquetas);

        } catch (DataAccessException | MongoException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Las estadísticas no están disponibles por el momento");
        }
    }

    private List<Document> ejecutar(Document... etapas) {
        return mongoTemplate.getCollection(COLECCION).aggregate(List.of(etapas)).into(new ArrayList<>());
    }

    /** Lee comida, servicio, ambiente y tiempoEspera de un documento agrupado y calcula el general. */
    private static Promedios promedios(Document d) {
        double comida = numero(d, "comida");
        double servicio = numero(d, "servicio");
        double ambiente = numero(d, "ambiente");
        double tiempo = numero(d, "tiempoEspera");
        return new Promedios(
                redondear(comida), redondear(servicio), redondear(ambiente), redondear(tiempo),
                redondear((comida + servicio + ambiente + tiempo) / 4));
    }

    private static double numero(Document d, String campo) {
        return d.get(campo) instanceof Number n ? n.doubleValue() : 0.0;
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}