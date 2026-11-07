package com.lamar.backend.controller;

import com.lamar.backend.dto.InsumoDto;
import com.lamar.backend.dto.InsumoRequests;
import com.lamar.backend.service.InsumoService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/insumos")
@RequiredArgsConstructor
public class InsumoController {

    private final InsumoService insumoService;

    @GetMapping
    public List<InsumoDto> listar(@RequestParam(required = false) String buscar) {
        return insumoService.listar(buscar);
    }

    @GetMapping("/{id}")
    public InsumoDto obtener(@PathVariable Integer id) {
        return insumoService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InsumoDto crear(@Valid @RequestBody InsumoRequests.Guardar req) {
        return insumoService.crear(req);
    }

    @PutMapping("/{id}")
    public InsumoDto actualizar(@PathVariable Integer id, @Valid @RequestBody InsumoRequests.Guardar req) {
        return insumoService.actualizar(id, req);
    }
}