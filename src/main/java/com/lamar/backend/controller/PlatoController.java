package com.lamar.backend.controller;

import com.lamar.backend.dto.PlatoDto;
import com.lamar.backend.service.PlatoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platos")
@RequiredArgsConstructor
public class PlatoController {

    private final PlatoService platoService;

    @GetMapping
    public List<PlatoDto> listar(@RequestParam(required = false) String categoria,
                                 @RequestParam(required = false) String buscar) {
        return platoService.listar(categoria, buscar);
    }

    @GetMapping("/categorias")
    public List<String> categorias() {
        return platoService.categorias();
    }

    @GetMapping("/{id}")
    public PlatoDto obtener(@PathVariable Integer id) {
        return platoService.obtener(id);
    }
}