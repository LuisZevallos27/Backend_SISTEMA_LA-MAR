package com.lamar.backend.controller;

import com.lamar.backend.dto.MesaDto;
import com.lamar.backend.service.MesaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mesas")
@RequiredArgsConstructor
public class MesaController {

    private final MesaService mesaService;

    @GetMapping
    public List<MesaDto> listar(@RequestParam(required = false) String zona) {
        return mesaService.listar(zona);
    }
}