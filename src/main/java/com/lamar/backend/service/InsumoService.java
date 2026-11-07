package com.lamar.backend.service;

import com.lamar.backend.dto.InsumoDto;
import com.lamar.backend.dto.InsumoRequests;
import com.lamar.backend.entity.Insumo;
import com.lamar.backend.repository.InsumoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class InsumoService {

    /** Recibido = compras en estado Recibido. Consumido = platos de pedidos no anulados x receta. */
    private static final String SQL_MOVIMIENTOS = """
            SELECT i.id_insumo AS id,
                   COALESCE((SELECT SUM(da.cantidad)
                               FROM Detalle_Abastecimiento da
                               JOIN Abastecimiento a ON a.id_abastecimiento = da.id_abastecimiento
                              WHERE da.id_insumo = i.id_insumo AND a.estado = 'Recibido'), 0) AS recibido,
                   COALESCE((SELECT SUM(dp.cantidad * r.cantidad_usada)
                               FROM Detalle_Pedido dp
                               JOIN Pedido p ON p.id_pedido = dp.id_pedido
                               JOIN Receta_Insumo r ON r.id_plato = dp.id_plato
                              WHERE r.id_insumo = i.id_insumo AND p.estado_pedido <> 'Anulado'), 0) AS consumido
              FROM Insumo i
            """;

    private final InsumoRepository insumoRepository;
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public List<InsumoDto> listar(String buscar) {
        List<Insumo> insumos = (buscar == null || buscar.isBlank())
                ? insumoRepository.findAllByOrderByNombreAsc()
                : insumoRepository.findByNombreContainingIgnoreCaseOrderByNombreAsc(buscar.trim());
        Map<Integer, BigDecimal[]> mov = movimientos();
        return insumos.stream().map(i -> toDto(i, mov)).toList();
    }

    @Transactional(readOnly = true)
    public InsumoDto obtener(Integer id) {
        return toDto(buscar(id), movimientos());
    }

    @Transactional
    public InsumoDto crear(InsumoRequests.Guardar req) {
        String nombre = req.nombre().trim();
        if (insumoRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un insumo con ese nombre");
        }
        Insumo i = new Insumo();
        i.setNombre(nombre);
        i.setUnidadMedida(req.unidadMedida().trim());
        insumoRepository.save(i);
        return toDto(i, movimientos());
    }

    @Transactional
    public InsumoDto actualizar(Integer id, InsumoRequests.Guardar req) {
        Insumo i = buscar(id);
        String nombre = req.nombre().trim();
        if (insumoRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un insumo con ese nombre");
        }
        i.setNombre(nombre);
        i.setUnidadMedida(req.unidadMedida().trim());
        return toDto(i, movimientos());
    }

    private Insumo buscar(Integer id) {
        return insumoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Insumo no encontrado"));
    }

    private Map<Integer, BigDecimal[]> movimientos() {
        Map<Integer, BigDecimal[]> mov = new HashMap<>();
        for (Map<String, Object> fila : jdbc.queryForList(SQL_MOVIMIENTOS)) {
            mov.put(((Number) fila.get("id")).intValue(),
                    new BigDecimal[] { dec(fila.get("recibido")), dec(fila.get("consumido")) });
        }
        return mov;
    }

    private InsumoDto toDto(Insumo i, Map<Integer, BigDecimal[]> mov) {
        BigDecimal[] v = mov.getOrDefault(i.getId(), new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
        BigDecimal recibido = v[0].setScale(3, RoundingMode.HALF_UP);
        BigDecimal consumido = v[1].setScale(3, RoundingMode.HALF_UP);
        return new InsumoDto(i.getId(), i.getNombre(), i.getUnidadMedida(),
                recibido, consumido, recibido.subtract(consumido));
    }

    private BigDecimal dec(Object o) {
        return new BigDecimal(o.toString());
    }
}