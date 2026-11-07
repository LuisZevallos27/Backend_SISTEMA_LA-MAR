package com.lamar.backend.controller;

import com.lamar.backend.dto.PrecuentaDto;
import com.lamar.backend.service.PrecuentaService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PrecuentaController {

    private final PrecuentaService precuentaService;

    @GetMapping("/{id}/precuenta")
    public PrecuentaDto precuenta(@PathVariable Integer id,
                                  @RequestParam(required = false) BigDecimal propina,
                                  @RequestParam(required = false) BigDecimal propinaPct) {
        return precuentaService.generar(id, propina, propinaPct);
    }
}