package com.lamar.backend.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

import com.lamar.backend.dto.ResenaPromedioDto;
import com.lamar.backend.dto.ResenaResumenDto;
import com.lamar.backend.dto.ResenaResumenDto.CategoriaCalificada;
import com.lamar.backend.dto.ResenaResumenDto.ConteoEstrellas;
import com.lamar.backend.dto.ResenaResumenDto.PlatoCalificado;
import com.mongodb.MongoException;

import lombok.RequiredArgsConstructor;

/**
 * Promedios y estadísticas de reseñas con el framework de agregación de
 * MongoDB.
 * Son las mismas consultas que se probaron en el script BD_LA_MAR_mongo.js.
 */
@Service
@RequiredArgsConstructor
public class ResenaResumenService {

    private static final String COLECCION = "resenas";

    /**
     * Un plato con menos reseñas que esto no entra a "mejores" ni "peores": pocos
     * votos no son representativos.
     */
    private static final int MIN_RESENAS_RANKING = 3;
    private static final int TOP = 5;
    private static final String SIN_CATEGORIA = "Sin categoría";

    private static final String GRUPO_PROMEDIOS = """
            { "$group": { "_id": "$idPlato", "promedio": { "$avg": "$estrellas" }, "resenas": { "$sum": 1 } } }
            """;

    private static final String ORDEN_POR_ID = """
            { "$sort": { "_id": 1 } }
            """;

    private static final String GRUPO_GENERAL = """
            { "$group": { "_id": null, "total": { "$sum": 1 }, "promedio": { "$avg": "$estrellas" } } }
            """;

    private static final String GRUPO_ESTRELLAS = """
            { "$group": { "_id": "$estrellas", "cantidad": { "$sum": 1 } } }
            """;

    /**
     * Se ordena por fecha antes de agrupar para que $last devuelva el nombre más
     * reciente del plato.
     */
    private static final String ORDEN_POR_FECHA = """
            { "$sort": { "fecha": 1 } }
            """;

    private static final String GRUPO_PLATOS = """
            { "$group": { "_id": "$idPlato", "plato": { "$last": "$nombrePlato" }, "categoria": { "$last": "$categoria" },
              "resenas": { "$sum": 1 }, "promedio": { "$avg": "$estrellas" } } }
            """;

    private static final String GRUPO_CATEGORIAS = """
            { "$group": { "_id": "$categoria", "resenas": { "$sum": 1 }, "promedio": { "$avg": "$estrellas" } } }
            """;

    private final MongoTemplate mongoTemplate;

    /**
     * Promedio de cada plato con al menos una reseña (todas las fechas), ordenado
     * por idPlato.
     */
    public List<ResenaPromedioDto> promedios() {
        try {
            return ejecutar(Document.parse(GRUPO_PROMEDIOS), Document.parse(ORDEN_POR_ID)).stream()
                    .map(d -> new ResenaPromedioDto(
                            ((Number) d.get("_id")).intValue(),
                            redondear(numero(d, "promedio")),
                            ((Number) d.get("resenas")).longValue()))
                    .toList();
        } catch (DataAccessException | MongoException ex) {
            throw noDisponible();
        }
    }

    public ResenaResumenDto resumen(int dias) {
        int periodo = Math.max(1, Math.min(dias, 365));
        Document filtro = new Document("$match", new Document("fecha",
                new Document("$gte", Date.from(Instant.now().minus(periodo, ChronoUnit.DAYS)))));

        try {
            // 1. Total y promedio general
            long total = 0;
            double promedioGeneral = 0;
            List<Document> general = ejecutar(filtro, Document.parse(GRUPO_GENERAL));
            if (!general.isEmpty()) {
                total = ((Number) general.get(0).get("total")).longValue();
                promedioGeneral = redondear(numero(general.get(0), "promedio"));
            }

            // 2. Distribución de estrellas (siempre de 5 a 1, aunque no haya votos)
            Map<Integer, Long> conteo = new HashMap<>();
            for (Document d : ejecutar(filtro, Document.parse(GRUPO_ESTRELLAS))) {
                conteo.put(((Number) d.get("_id")).intValue(), ((Number) d.get("cantidad")).longValue());
            }
            List<ConteoEstrellas> distribucion = new ArrayList<>();
            for (int estrellas = 5; estrellas >= 1; estrellas--) {
                distribucion.add(new ConteoEstrellas(estrellas, conteo.getOrDefault(estrellas, 0L)));
            }

            // 3. Platos calificados (con suficientes reseñas), del mejor al peor
            List<PlatoCalificado> calificados = ejecutar(
                    filtro, Document.parse(ORDEN_POR_FECHA), Document.parse(GRUPO_PLATOS)).stream()
                    .filter(d -> numero(d, "resenas") >= MIN_RESENAS_RANKING)
                    .map(d -> new PlatoCalificado(
                            ((Number) d.get("_id")).intValue(),
                            d.getString("plato"),
                            d.getString("categoria"),
                            ((Number) d.get("resenas")).longValue(),
                            redondear(numero(d, "promedio"))))
                    .sorted(Comparator.comparingDouble(PlatoCalificado::promedio).reversed()
                            .thenComparing(Comparator.comparingLong(PlatoCalificado::resenas).reversed())
                            .thenComparing(PlatoCalificado::plato, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();

            // Los "peores" nunca repiten a los "mejores": con pocos platos, los mejores van
            // primero
            int nMejores = Math.min(TOP, calificados.size());
            List<PlatoCalificado> mejores = new ArrayList<>(calificados.subList(0, nMejores));
            int nPeores = Math.min(TOP, calificados.size() - nMejores);
            List<PlatoCalificado> peores = new ArrayList<>(
                    calificados.subList(calificados.size() - nPeores, calificados.size()));
            Collections.reverse(peores); // el peor primero

            // 4. Promedio por categoría
            List<CategoriaCalificada> categorias = ejecutar(filtro, Document.parse(GRUPO_CATEGORIAS)).stream()
                    .map(d -> new CategoriaCalificada(
                            d.getString("_id") == null ? SIN_CATEGORIA : d.getString("_id"),
                            ((Number) d.get("resenas")).longValue(),
                            redondear(numero(d, "promedio"))))
                    .sorted(Comparator.comparingDouble(CategoriaCalificada::promedio).reversed()
                            .thenComparing(CategoriaCalificada::categoria))
                    .toList();

            return new ResenaResumenDto(periodo, total, promedioGeneral, distribucion, mejores, peores, categorias);

        } catch (DataAccessException | MongoException ex) {
            throw noDisponible();
        }
    }

    private List<Document> ejecutar(Document... etapas) {
        return mongoTemplate.getCollection(COLECCION).aggregate(List.of(etapas)).into(new ArrayList<>());
    }

    private static double numero(Document d, String campo) {
        return d.get(campo) instanceof Number n ? n.doubleValue() : 0.0;
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private static ResponseStatusException noDisponible() {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "Las estadísticas no están disponibles por el momento");
    }
}