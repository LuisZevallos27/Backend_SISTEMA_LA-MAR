package com.lamar.backend.controller;

import com.lamar.backend.dto.ClienteDto;
import com.lamar.backend.dto.ClienteRequests;
import com.lamar.backend.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping("/{dni}")
    public ClienteDto buscar(@PathVariable String dni) {
        return clienteService.buscarPorDni(dni);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteDto crear(@Valid @RequestBody ClienteRequests.Crear req) {
        return clienteService.crear(req);
    }

    @PutMapping("/{id}")
    public ClienteDto actualizar(@PathVariable Integer id,
                                 @Valid @RequestBody ClienteRequests.Actualizar req) {
        return clienteService.actualizar(id, req);
    }
}