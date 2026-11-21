package com.lamar.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lamar.backend.dto.EncuestaDto;
import com.lamar.backend.dto.EncuestaRequests;
import com.lamar.backend.dto.EncuestaResumenDto;
import com.lamar.backend.service.EncuestaResumenService;
import com.lamar.backend.service.EncuestaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/encuestas")
@RequiredArgsConstructor
public class EncuestaController {

    private final EncuestaService encuestaService;
    private final EncuestaResumenService encuestaResumenService;

    @GetMapping
    public List<EncuestaDto> listar(@RequestParam(defaultValue = "20") int limite) {
        return encuestaService.listar(limite);
    }

    @GetMapping("/resumen")
    public EncuestaResumenDto resumen(@RequestParam(defaultValue = "30") int dias) {
        return encuestaResumenService.resumen(dias);
    }

    @GetMapping("/pedido/{idPedido}")
    public EncuestaDto obtenerPorPedido(@PathVariable Integer idPedido) {
        return encuestaService.obtenerPorPedido(idPedido);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EncuestaDto crear(@Valid @RequestBody EncuestaRequests.Crear req) {
        return encuestaService.crear(req);
    }
}