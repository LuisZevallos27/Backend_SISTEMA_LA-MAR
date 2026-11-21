package com.lamar.backend.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final JdbcTemplate jdbc;
    private final MongoTemplate mongo;

    public HealthController(JdbcTemplate jdbc, MongoTemplate mongo) {
        this.jdbc = jdbc;
        this.mongo = mongo;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Integer mesas = jdbc.queryForObject("SELECT COUNT(*) FROM Mesa", Integer.class);
        String horaSql = jdbc.queryForObject("SELECT CONVERT(VARCHAR(19), GETDATE(), 120)", String.class);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("status", "ok");
        r.put("mesas", mesas);
        r.put("zona", ZoneId.systemDefault().getId());
        r.put("hora", LocalDateTime.now().withNano(0).toString());
        r.put("horaServidorSql", horaSql);
        r.put("mongo", estadoMongo());
        return r;
    }

    /**
     * MongoDB solo guarda encuestas y reseñas: si está caído, el restaurante
     * sigue funcionando. Por eso aquí un fallo NO cambia "status" ni lanza error.
     */
    private Map<String, Object> estadoMongo() {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            long encuestas = mongo.getCollection("encuestas").countDocuments();
            long resenas = mongo.getCollection("resenas").countDocuments();
            m.put("estado", "ok");
            m.put("base", mongo.getDb().getName());
            m.put("encuestas", encuestas);
            m.put("resenas", resenas);
        } catch (Exception e) {
            m.clear();
            m.put("estado", "caido");
        }
        return m;
    }
}