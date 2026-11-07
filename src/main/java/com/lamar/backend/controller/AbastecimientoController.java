package com.lamar.backend.controller;

import com.lamar.backend.dto.AbastecimientoDto;
import com.lamar.backend.dto.AbastecimientoRequests;
import com.lamar.backend.service.AbastecimientoService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/abastecimientos")
@RequiredArgsConstructor
public class AbastecimientoController {

    private final AbastecimientoService abastecimientoService;

    @GetMapping
    public List<AbastecimientoDto> listar(@RequestParam(required = false) String estado) {
        return abastecimientoService.listar(estado);
    }

    @GetMapping("/{id}")
    public AbastecimientoDto obtener(@PathVariable Integer id) {
        return abastecimientoService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AbastecimientoDto crear(@Valid @RequestBody AbastecimientoRequests.Crear req) {
        return abastecimientoService.crear(req);
    }

    @PostMapping("/{id}/recibir")
    public AbastecimientoDto recibir(@PathVariable Integer id) {
        return abastecimientoService.recibir(id);
    }

    @PostMapping("/{id}/anular")
    public AbastecimientoDto anular(@PathVariable Integer id) {
        return abastecimientoService.anular(id);
    }
}