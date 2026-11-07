package com.lamar.backend.service;

import com.lamar.backend.dto.AbastecimientoDto;
import com.lamar.backend.dto.AbastecimientoRequests;
import com.lamar.backend.entity.Abastecimiento;
import com.lamar.backend.entity.DetalleAbastecimiento;
import com.lamar.backend.entity.Insumo;
import com.lamar.backend.entity.Proveedor;
import com.lamar.backend.repository.AbastecimientoRepository;
import com.lamar.backend.repository.InsumoRepository;
import com.lamar.backend.repository.ProveedorRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AbastecimientoService {

    private static final String PENDIENTE = "Pendiente";
    private static final String RECIBIDO = "Recibido";
    private static final String ANULADO = "Anulado";

    private final AbastecimientoRepository abastecimientoRepository;
    private final ProveedorRepository proveedorRepository;
    private final InsumoRepository insumoRepository;

    @Transactional(readOnly = true)
    public List<AbastecimientoDto> listar(String estado) {
        List<Abastecimiento> lista = (estado == null || estado.isBlank())
                ? abastecimientoRepository.findAllByOrderByFechaDesc()
                : abastecimientoRepository.findByEstadoOrderByFechaDesc(estado.trim());
        return lista.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public AbastecimientoDto obtener(Integer id) {
        return toDto(buscar(id));
    }

    @Transactional
    public AbastecimientoDto crear(AbastecimientoRequests.Crear req) {
        Proveedor proveedor = proveedorRepository.findById(req.idProveedor())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proveedor no encontrado"));

        Set<Integer> vistos = new HashSet<>();
        for (AbastecimientoRequests.Item item : req.items()) {
            if (!vistos.add(item.idInsumo())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El insumo " + item.idInsumo() + " esta repetido en la orden");
            }
        }

        Abastecimiento a = new Abastecimiento();
        a.setProveedor(proveedor);
        a.setFecha(LocalDateTime.now());
        a.setEstado(PENDIENTE);
        abastecimientoRepository.save(a);

        for (AbastecimientoRequests.Item item : req.items()) {
            Insumo insumo = insumoRepository.findById(item.idInsumo())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Insumo no encontrado: " + item.idInsumo()));
            DetalleAbastecimiento d = new DetalleAbastecimiento();
            d.getId().setIdAbastecimiento(a.getId());
            d.getId().setIdInsumo(insumo.getId());
            d.setAbastecimiento(a);
            d.setInsumo(insumo);
            d.setCantidad(item.cantidad().setScale(2, RoundingMode.HALF_UP));
            d.setCostoTotal(item.costoTotal().setScale(2, RoundingMode.HALF_UP));
            a.getDetalles().add(d);
        }
        return toDto(a);
    }

    @Transactional
    public AbastecimientoDto recibir(Integer id) {
        Abastecimiento a = buscar(id);
        if (!PENDIENTE.equals(a.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede recibir una orden pendiente");
        }
        a.setEstado(RECIBIDO);
        return toDto(a);
    }

    @Transactional
    public AbastecimientoDto anular(Integer id) {
        Abastecimiento a = buscar(id);
        if (!PENDIENTE.equals(a.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede anular una orden pendiente");
        }
        a.setEstado(ANULADO);
        return toDto(a);
    }

    private Abastecimiento buscar(Integer id) {
        return abastecimientoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada"));
    }

    private AbastecimientoDto toDto(Abastecimiento a) {
        List<AbastecimientoDto.Item> items = a.getDetalles().stream()
                .sorted(Comparator.comparing((DetalleAbastecimiento d) -> d.getInsumo().getNombre()))
                .map(d -> new AbastecimientoDto.Item(
                        d.getInsumo().getId(),
                        d.getInsumo().getNombre(),
                        d.getInsumo().getUnidadMedida(),
                        d.getCantidad(),
                        d.getCantidad().signum() == 0
                                ? BigDecimal.ZERO.setScale(2)
                                : d.getCostoTotal().divide(d.getCantidad(), 2, RoundingMode.HALF_UP),
                        d.getCostoTotal()))
                .toList();
        BigDecimal total = items.stream()
                .map(AbastecimientoDto.Item::costoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AbastecimientoDto(a.getId(), a.getProveedor().getId(),
                a.getProveedor().getRazonSocial(), a.getProveedor().getRuc(),
                a.getFecha(), a.getEstado(), items, total);
    }
}