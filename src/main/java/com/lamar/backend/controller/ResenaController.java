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

import com.lamar.backend.dto.ResenaDto;
import com.lamar.backend.dto.ResenaPromedioDto;
import com.lamar.backend.dto.ResenaRequests;
import com.lamar.backend.dto.ResenaResumenDto;
import com.lamar.backend.service.ResenaResumenService;
import com.lamar.backend.service.ResenaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/resenas")
@RequiredArgsConstructor
public class ResenaController {

    private final ResenaService resenaService;
    private final ResenaResumenService resenaResumenService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<ResenaDto> crear(@Valid @RequestBody ResenaRequests.Crear req) {
        return resenaService.crear(req);
    }

    @GetMapping("/pedido/{idPedido}")
    public List<ResenaDto> porPedido(@PathVariable Integer idPedido) {
        return resenaService.listarPorPedido(idPedido);
    }

    @GetMapping("/plato/{idPlato}")
    public List<ResenaDto> porPlato(@PathVariable Integer idPlato,
            @RequestParam(defaultValue = "10") int limite) {
        return resenaService.listarPorPlato(idPlato, limite);
    }

    @GetMapping("/promedios")
    public List<ResenaPromedioDto> promedios() {
        return resenaResumenService.promedios();
    }

    @GetMapping("/resumen")
    public ResenaResumenDto resumen(@RequestParam(defaultValue = "30") int dias) {
        return resenaResumenService.resumen(dias);
    }
}