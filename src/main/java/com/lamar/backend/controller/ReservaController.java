package com.lamar.backend.controller;

import com.lamar.backend.dto.ReservaDto;
import com.lamar.backend.dto.ReservaRequests;
import com.lamar.backend.service.ReservaService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    @GetMapping
    public List<ReservaDto> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) String estado) {
        return reservaService.listar(fecha, estado);
    }

    @GetMapping("/{id}")
    public ReservaDto obtener(@PathVariable Integer id) {
        return reservaService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaDto crear(@Valid @RequestBody ReservaRequests.Crear req) {
        return reservaService.crear(req);
    }

    @PostMapping("/{id}/confirmar")
    public ReservaDto confirmar(@PathVariable Integer id) {
        return reservaService.confirmar(id);
    }

    @PostMapping("/{id}/cancelar")
    public ReservaDto cancelar(@PathVariable Integer id) {
        return reservaService.cancelar(id);
    }

    @PostMapping("/{id}/atender")
    public ReservaDto atender(@PathVariable Integer id) {
        return reservaService.atender(id);
    }
}