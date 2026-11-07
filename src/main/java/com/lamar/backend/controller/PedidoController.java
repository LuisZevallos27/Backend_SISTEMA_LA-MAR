package com.lamar.backend.controller;

import com.lamar.backend.dto.PedidoDto;
import com.lamar.backend.dto.PedidoRequests;
import com.lamar.backend.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pedidos") 
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoDto abrir(@AuthenticationPrincipal Jwt jwt,
                           @Valid @RequestBody PedidoRequests.Abrir req) {
        return pedidoService.abrir(Integer.valueOf(jwt.getSubject()), req);
    }

    @GetMapping("/{id}")
    public PedidoDto obtener(@PathVariable Integer id) {
        return pedidoService.obtener(id);
    }

    @GetMapping("/mesa/{idMesa}/abierto")
    public PedidoDto abiertoPorMesa(@PathVariable Integer idMesa) {
        return pedidoService.obtenerAbiertoPorMesa(idMesa);
    }

    @PostMapping("/{id}/items")
    public PedidoDto agregarItem(@PathVariable Integer id,
                                 @Valid @RequestBody PedidoRequests.AgregarItem req) {
        return pedidoService.agregarItem(id, req);
    }

    @PutMapping("/{id}/items/{idPlato}")
    public PedidoDto cambiarCantidad(@PathVariable Integer id, @PathVariable Integer idPlato,
                                     @Valid @RequestBody PedidoRequests.CambiarCantidad req) {
        return pedidoService.cambiarCantidad(id, idPlato, req);
    }

    @DeleteMapping("/{id}/items/{idPlato}")
    public PedidoDto quitarItem(@PathVariable Integer id, @PathVariable Integer idPlato) {
        return pedidoService.quitarItem(id, idPlato);
    }

    @PutMapping("/{id}/cliente")
    public PedidoDto asignarCliente(@PathVariable Integer id,
                                    @Valid @RequestBody PedidoRequests.AsignarCliente req) {
        return pedidoService.asignarCliente(id, req);
    }

    @PostMapping("/{id}/cerrar")
    public PedidoDto cerrar(@PathVariable Integer id,
                            @RequestBody(required = false) PedidoRequests.Cerrar req) {
        return pedidoService.cerrar(id, req);
    }

    @PostMapping("/{id}/anular")
    public PedidoDto anular(@PathVariable Integer id) {
        return pedidoService.anular(id);
    }
}