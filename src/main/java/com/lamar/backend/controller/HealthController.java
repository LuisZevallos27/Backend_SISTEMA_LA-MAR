package com.lamar.backend.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
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
        return r;
    }
}