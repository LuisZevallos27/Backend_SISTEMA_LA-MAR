package com.lamar.backend.controller;

import com.lamar.backend.dto.ProveedorDto;
import com.lamar.backend.dto.ProveedorRequests;
import com.lamar.backend.service.ProveedorService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService proveedorService;

    @GetMapping
    public List<ProveedorDto> listar() {
        return proveedorService.listar();
    }

    @GetMapping("/{id}")
    public ProveedorDto obtener(@PathVariable Integer id) {
        return proveedorService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProveedorDto crear(@Valid @RequestBody ProveedorRequests.Crear req) {
        return proveedorService.crear(req);
    }

    @PutMapping("/{id}")
    public ProveedorDto actualizar(@PathVariable Integer id,
                                   @Valid @RequestBody ProveedorRequests.Actualizar req) {
        return proveedorService.actualizar(id, req);
    }
}