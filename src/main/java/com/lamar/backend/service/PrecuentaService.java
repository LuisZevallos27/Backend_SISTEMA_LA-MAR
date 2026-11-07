package com.lamar.backend.service;

import com.lamar.backend.dto.PedidoDto;
import com.lamar.backend.dto.PrecuentaDto;
import com.lamar.backend.util.NumeroALetras;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PrecuentaService {

    private final PedidoService pedidoService;

    public PrecuentaDto generar(Integer idPedido, BigDecimal propina, BigDecimal propinaPct) {
        if (propina != null && propinaPct != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Envie propina o propinaPct, no ambos");
        }

        PedidoDto p = pedidoService.obtener(idPedido);

        BigDecimal montoPropina = BigDecimal.ZERO.setScale(2);
        if (propina != null) {
            if (propina.signum() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La propina no puede ser negativa");
            }
            montoPropina = propina.setScale(2, RoundingMode.HALF_UP);
        } else if (propinaPct != null) {
            if (propinaPct.signum() < 0 || propinaPct.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El porcentaje de propina debe estar entre 0 y 100");
            }
            montoPropina = p.baseImponible().multiply(propinaPct)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        BigDecimal totalAPagar = p.total().add(montoPropina);

        List<PrecuentaDto.Linea> lineas = p.items().stream()
                .map(i -> new PrecuentaDto.Linea(i.nombre(), i.cantidad(), i.precioUnitario(), i.subtotal()))
                .toList();

        return new PrecuentaDto(
                p.id(),
                p.estado(),
                p.tipoComprobante(),
                p.fechaHora() == null ? null : p.fechaHora().toLocalDate(),
                p.fechaHora() == null ? null : p.fechaHora().toLocalTime().withNano(0),
                p.numeroMesa(),
                p.zona(),
                p.cliente(),
                p.clienteDni(),
                p.empleado(),
                lineas,
                p.baseImponible(),
                p.igv(),
                p.total(),
                montoPropina,
                totalAPagar,
                "SON: " + NumeroALetras.soles(totalAPagar));
    }
}