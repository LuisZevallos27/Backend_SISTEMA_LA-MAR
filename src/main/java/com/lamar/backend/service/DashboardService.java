package com.lamar.backend.service;

import com.lamar.backend.dto.DashboardDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

        /**
         * Un insumo tiene stock bajo si alcanza para menos de estos dias de consumo.
         */
        private static final BigDecimal DIAS_STOCK_BAJO = BigDecimal.valueOf(3);
        private static final BigDecimal CIEN = BigDecimal.valueOf(100);
        private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2);

        // Las fechas se envian como parametros (hora de Lima) y se convierten a
        // DATETIME para poder usar el indice
        private static final String SQL_KPI_HOY = """
                        SELECT COALESCE(SUM(CASE WHEN estado_pedido = 'Cerrado' THEN monto_total END), 0) AS ventas,
                               COUNT(CASE WHEN estado_pedido = 'Cerrado' THEN 1 END) AS cerrados,
                               COUNT(CASE WHEN estado_pedido = 'Anulado' THEN 1 END) AS anulados
                          FROM Pedido
                         WHERE fecha_hora >= CAST(? AS DATETIME) AND fecha_hora < CAST(? AS DATETIME)
                        """;

        private static final String SQL_VENTAS_RANGO = """
                        SELECT COALESCE(SUM(monto_total), 0)
                          FROM Pedido
                         WHERE estado_pedido = 'Cerrado'
                           AND fecha_hora >= CAST(? AS DATETIME) AND fecha_hora < CAST(? AS DATETIME)
                        """;

        private static final String SQL_ABIERTOS = """
                        SELECT COUNT(*) AS cantidad, COALESCE(SUM(monto_total), 0) AS monto
                          FROM Pedido
                         WHERE estado_pedido = 'Abierto'
                        """;

        private static final String SQL_POR_HORA = """
                        SELECT DATEPART(HOUR, fecha_hora) AS hora, COUNT(*) AS pedidos, SUM(monto_total) AS ventas
                          FROM Pedido
                         WHERE estado_pedido = 'Cerrado'
                           AND fecha_hora >= CAST(? AS DATETIME) AND fecha_hora < CAST(? AS DATETIME)
                         GROUP BY DATEPART(HOUR, fecha_hora)
                        """;

        private static final String SQL_POR_DIA = """
                        SELECT CAST(fecha_hora AS DATE) AS dia, COUNT(*) AS pedidos, SUM(monto_total) AS ventas
                          FROM Pedido
                         WHERE estado_pedido = 'Cerrado'
                           AND fecha_hora >= CAST(? AS DATETIME) AND fecha_hora < CAST(? AS DATETIME)
                         GROUP BY CAST(fecha_hora AS DATE)
                        """;

        private static final String SQL_TOP_PLATOS = """
                        SELECT TOP 5 p.id_plato, p.nombre_plato, COALESCE(p.categoria, 'Sin categoría') AS categoria,
                               SUM(d.cantidad) AS unidades, SUM(d.subtotal) AS ventas
                          FROM Detalle_Pedido d
                          JOIN Pedido pe ON pe.id_pedido = d.id_pedido
                          JOIN Plato p ON p.id_plato = d.id_plato
                         WHERE pe.estado_pedido = 'Cerrado'
                           AND pe.fecha_hora >= CAST(? AS DATETIME) AND pe.fecha_hora < CAST(? AS DATETIME)
                         GROUP BY p.id_plato, p.nombre_plato, p.categoria
                         ORDER BY SUM(d.cantidad) DESC, SUM(d.subtotal) DESC
                        """;
        /** Cantidad de cada insumo que viene en ordenes de compra pendientes. */
        private static final String SQL_EN_CAMINO = """
                        SELECT da.id_insumo, SUM(da.cantidad) AS cantidad
                          FROM Detalle_Abastecimiento da
                          JOIN Abastecimiento a ON a.id_abastecimiento = da.id_abastecimiento
                         WHERE a.estado = 'Pendiente'
                         GROUP BY da.id_insumo
                        """;
        private static final String SQL_CATEGORIAS = """
                        SELECT COALESCE(p.categoria, 'Sin categoría') AS categoria,
                               SUM(d.cantidad) AS unidades, SUM(d.subtotal) AS ventas
                          FROM Detalle_Pedido d
                          JOIN Pedido pe ON pe.id_pedido = d.id_pedido
                          JOIN Plato p ON p.id_plato = d.id_plato
                         WHERE pe.estado_pedido = 'Cerrado'
                           AND pe.fecha_hora >= CAST(? AS DATETIME) AND pe.fecha_hora < CAST(? AS DATETIME)
                         GROUP BY COALESCE(p.categoria, 'Sin categoría')
                         ORDER BY SUM(d.subtotal) DESC
                        """;

        private static final String SQL_MOZOS = """
                        SELECT e.id_empleado, e.nombres_empleado + ' ' + e.apellidos_empleado AS nombre,
                               COUNT(*) AS pedidos, SUM(pe.monto_total) AS ventas
                          FROM Pedido pe
                          JOIN Empleado e ON e.id_empleado = pe.id_empleado
                         WHERE pe.estado_pedido = 'Cerrado'
                           AND pe.fecha_hora >= CAST(? AS DATETIME) AND pe.fecha_hora < CAST(? AS DATETIME)
                         GROUP BY e.id_empleado, e.nombres_empleado, e.apellidos_empleado
                         ORDER BY SUM(pe.monto_total) DESC
                        """;

        private static final String SQL_MESAS = """
                        SELECT m.id_mesa, m.numero_mesa, m.zona_mesa, m.capacidad, m.estado,
                               pe.id_pedido, pe.monto_total, pe.fecha_hora
                          FROM Mesa m
                          LEFT JOIN Pedido pe ON pe.id_mesa = m.id_mesa AND pe.estado_pedido = 'Abierto'
                         ORDER BY m.zona_mesa, m.numero_mesa
                        """;

        private static final String SQL_RESERVAS_HOY = """
                        SELECT r.id_reserva, r.hora_reserva, m.numero_mesa, m.zona_mesa,
                               c.nombres_cliente + ' ' + c.apellidos_cliente AS cliente,
                               r.cant_personas, r.estado, r.canal_reserva
                          FROM Reserva r
                          JOIN Mesa m ON m.id_mesa = r.id_mesa
                          JOIN Cliente c ON c.id_cliente = r.id_cliente
                         WHERE r.fecha_reserva = ?
                         ORDER BY r.hora_reserva, m.numero_mesa
                        """;

        private static final String SQL_RESERVAS_MANANA = """
                        SELECT COUNT(*) FROM Reserva
                         WHERE fecha_reserva = ? AND estado IN ('Pendiente', 'Confirmada')
                        """;

        private static final String SQL_COMPRAS_PENDIENTES = """
                        SELECT COUNT(DISTINCT a.id_abastecimiento) AS ordenes, COALESCE(SUM(da.costo_total), 0) AS monto
                          FROM Abastecimiento a
                          LEFT JOIN Detalle_Abastecimiento da ON da.id_abastecimiento = a.id_abastecimiento
                         WHERE a.estado = 'Pendiente'
                        """;

        private static final String SQL_ORDENES_PENDIENTES = """
                        SELECT TOP 5 a.id_abastecimiento, pr.prov_razon_social, a.fecha, COALESCE(SUM(da.costo_total), 0) AS monto
                          FROM Abastecimiento a
                          JOIN Proveedor pr ON pr.id_proveedor = a.id_proveedor
                          LEFT JOIN Detalle_Abastecimiento da ON da.id_abastecimiento = a.id_abastecimiento
                         WHERE a.estado = 'Pendiente'
                         GROUP BY a.id_abastecimiento, pr.prov_razon_social, a.fecha
                         ORDER BY a.fecha DESC
                        """;

        private static final String SQL_COMPRAS_PERIODO = """
                        SELECT COALESCE(SUM(da.costo_total), 0)
                          FROM Detalle_Abastecimiento da
                          JOIN Abastecimiento a ON a.id_abastecimiento = da.id_abastecimiento
                         WHERE a.estado = 'Recibido'
                           AND a.fecha >= CAST(? AS DATETIME) AND a.fecha < CAST(? AS DATETIME)
                        """;

        /**
         * Mismo calculo de stock que InsumoService, mas el consumo de la ultima semana.
         */
        private static final String SQL_STOCK = """
                        SELECT i.id_insumo, i.nombre_insumo, i.unidad_medida,
                               COALESCE((SELECT SUM(da.cantidad)
                                           FROM Detalle_Abastecimiento da
                                           JOIN Abastecimiento a ON a.id_abastecimiento = da.id_abastecimiento
                                          WHERE da.id_insumo = i.id_insumo AND a.estado = 'Recibido'), 0) AS recibido,
                               COALESCE((SELECT SUM(dp.cantidad * r.cantidad_usada)
                                           FROM Detalle_Pedido dp
                                           JOIN Pedido p ON p.id_pedido = dp.id_pedido
                                           JOIN Receta_Insumo r ON r.id_plato = dp.id_plato
                                          WHERE r.id_insumo = i.id_insumo AND p.estado_pedido <> 'Anulado'), 0) AS consumido,
                               COALESCE((SELECT SUM(dp.cantidad * r.cantidad_usada)
                                           FROM Detalle_Pedido dp
                                           JOIN Pedido p ON p.id_pedido = dp.id_pedido
                                           JOIN Receta_Insumo r ON r.id_plato = dp.id_plato
                                          WHERE r.id_insumo = i.id_insumo AND p.estado_pedido <> 'Anulado'
                                            AND p.fecha_hora >= CAST(? AS DATETIME)), 0) AS consumo_semana
                          FROM Insumo i
                        """;

        private record Kpi(BigDecimal ventas, int cerrados, int anulados) {
        }

        private record Abiertos(int cantidad, BigDecimal monto) {
        }

        private record FilaHora(int hora, int pedidos, BigDecimal ventas) {
        }

        private record Pendientes(int ordenes, BigDecimal monto) {
        }

        private final JdbcTemplate jdbc;

        @Transactional(readOnly = true)
        public DashboardDto resumen(int diasSolicitados) {
                int dias = Math.max(1, Math.min(diasSolicitados, 90));
                LocalDateTime ahora = LocalDateTime.now().withNano(0); // hora de Lima (ver BackendApplication)
                LocalDate hoy = ahora.toLocalDate();
                LocalDateTime inicioHoy = hoy.atStartOfDay();
                LocalDateTime inicioManana = inicioHoy.plusDays(1);
                LocalDateTime desde = inicioHoy.minusDays(dias - 1L);

                List<DashboardDto.MesaMapa> mesas = mesas(ahora);

                return new DashboardDto(
                                ahora,
                                dias,
                                hoy(ahora, inicioHoy, inicioManana),
                                ocupacion(mesas),
                                ventasPorDia(desde, inicioManana, desde.toLocalDate(), hoy),
                                ventasPorHora(inicioHoy, inicioManana, inicioHoy.minusDays(dias), inicioHoy, dias),
                                topPlatos(desde, inicioManana),
                                categorias(desde, inicioManana),
                                mozos(desde, inicioManana),
                                mesas,
                                reservas(hoy),
                                compras(desde, inicioManana, inicioHoy.minusDays(7)));
        }

        // ------------------------------------------------------------------ hoy
        private DashboardDto.Hoy hoy(LocalDateTime ahora, LocalDateTime inicioHoy, LocalDateTime inicioManana) {
                Kpi kpi = jdbc.queryForObject(SQL_KPI_HOY,
                                (rs, n) -> new Kpi(rs.getBigDecimal("ventas"), rs.getInt("cerrados"),
                                                rs.getInt("anulados")),
                                inicioHoy, inicioManana);
                // Ayer hasta esta misma hora, para comparar tramos equivalentes
                BigDecimal ayer = jdbc.queryForObject(SQL_VENTAS_RANGO, BigDecimal.class,
                                inicioHoy.minusDays(1), ahora.minusDays(1));
                Abiertos abiertos = jdbc.queryForObject(SQL_ABIERTOS,
                                (rs, n) -> new Abiertos(rs.getInt("cantidad"), rs.getBigDecimal("monto")));

                BigDecimal variacion = ayer.signum() == 0 ? null
                                : kpi.ventas().subtract(ayer).multiply(CIEN).divide(ayer, 1, RoundingMode.HALF_UP);
                BigDecimal ticket = kpi.cerrados() == 0 ? CERO
                                : kpi.ventas().divide(BigDecimal.valueOf(kpi.cerrados()), 2, RoundingMode.HALF_UP);

                return new DashboardDto.Hoy(kpi.ventas(), ayer, variacion, kpi.cerrados(), kpi.anulados(),
                                ticket, abiertos.cantidad(), abiertos.monto());
        }

        // ------------------------------------------------------------------ ocupacion
        // y mapa de mesas
        private List<DashboardDto.MesaMapa> mesas(LocalDateTime ahora) {
                return jdbc.query(SQL_MESAS, (rs, n) -> {
                        LocalDateTime abierto = rs.getObject("fecha_hora", LocalDateTime.class);
                        Integer minutos = abierto == null ? null
                                        : (int) Math.max(0, Duration.between(abierto, ahora).toMinutes());
                        return new DashboardDto.MesaMapa(
                                        rs.getInt("id_mesa"), rs.getInt("numero_mesa"), rs.getString("zona_mesa"),
                                        rs.getInt("capacidad"), rs.getString("estado"),
                                        rs.getObject("id_pedido", Integer.class), rs.getBigDecimal("monto_total"),
                                        minutos);
                });
        }

        private DashboardDto.Ocupacion ocupacion(List<DashboardDto.MesaMapa> mesas) {
                int total = mesas.size();
                int disponibles = (int) mesas.stream().filter(m -> "Disponible".equals(m.estado())).count();
                int reservadas = (int) mesas.stream().filter(m -> "Reservado".equals(m.estado())).count();
                int ocupadas = (int) mesas.stream().filter(m -> "Ocupado".equals(m.estado())).count();
                int porcentaje = total == 0 ? 0 : Math.round(100f * ocupadas / total);
                return new DashboardDto.Ocupacion(total, disponibles, reservadas, ocupadas, porcentaje);
        }

        // ------------------------------------------------------------------ ventas por
        // dia y por hora
        private List<DashboardDto.VentaDia> ventasPorDia(LocalDateTime desde, LocalDateTime hasta,
                        LocalDate primerDia, LocalDate ultimoDia) {
                Map<LocalDate, DashboardDto.VentaDia> porDia = new HashMap<>();
                for (DashboardDto.VentaDia v : jdbc.query(SQL_POR_DIA,
                                (rs, n) -> new DashboardDto.VentaDia(rs.getObject("dia", LocalDate.class),
                                                rs.getInt("pedidos"), rs.getBigDecimal("ventas")),
                                desde, hasta)) {
                        porDia.put(v.dia(), v);
                }
                List<DashboardDto.VentaDia> lista = new ArrayList<>();
                for (LocalDate d = primerDia; !d.isAfter(ultimoDia); d = d.plusDays(1)) {
                        lista.add(porDia.getOrDefault(d, new DashboardDto.VentaDia(d, 0, CERO)));
                }
                return lista;
        }

        private List<DashboardDto.VentaHora> ventasPorHora(LocalDateTime inicioHoy, LocalDateTime inicioManana,
                        LocalDateTime desdePrevios, LocalDateTime hastaPrevios,
                        int dias) {
                Map<Integer, FilaHora> deHoy = filasPorHora(inicioHoy, inicioManana);
                Map<Integer, FilaHora> previos = filasPorHora(desdePrevios, hastaPrevios);

                int min = 11; // horario habitual del local; se amplia si hay ventas fuera de el
                int max = 23;
                for (int h : deHoy.keySet()) {
                        min = Math.min(min, h);
                        max = Math.max(max, h);
                }
                for (int h : previos.keySet()) {
                        min = Math.min(min, h);
                        max = Math.max(max, h);
                }

                List<DashboardDto.VentaHora> lista = new ArrayList<>();
                for (int h = min; h <= max; h++) {
                        FilaHora a = deHoy.get(h);
                        FilaHora b = previos.get(h);
                        BigDecimal promedio = b == null ? CERO
                                        : b.ventas().divide(BigDecimal.valueOf(dias), 2, RoundingMode.HALF_UP);
                        lista.add(new DashboardDto.VentaHora(h, a == null ? 0 : a.pedidos(),
                                        a == null ? CERO : a.ventas(),
                                        promedio));
                }
                return lista;
        }

        private Map<Integer, FilaHora> filasPorHora(LocalDateTime desde, LocalDateTime hasta) {
                Map<Integer, FilaHora> mapa = new HashMap<>();
                for (FilaHora f : jdbc.query(SQL_POR_HORA,
                                (rs, n) -> new FilaHora(rs.getInt("hora"), rs.getInt("pedidos"),
                                                rs.getBigDecimal("ventas")),
                                desde, hasta)) {
                        mapa.put(f.hora(), f);
                }
                return mapa;
        }

        // ------------------------------------------------------------------ platos,
        // categorias y mozos
        private List<DashboardDto.PlatoTop> topPlatos(LocalDateTime desde, LocalDateTime hasta) {
                return jdbc.query(SQL_TOP_PLATOS,
                                (rs, n) -> new DashboardDto.PlatoTop(rs.getInt("id_plato"),
                                                rs.getString("nombre_plato"),
                                                rs.getString("categoria"), rs.getInt("unidades"),
                                                rs.getBigDecimal("ventas")),
                                desde, hasta);
        }

        private List<DashboardDto.Categoria> categorias(LocalDateTime desde, LocalDateTime hasta) {
                return jdbc.query(SQL_CATEGORIAS,
                                (rs, n) -> new DashboardDto.Categoria(rs.getString("categoria"), rs.getInt("unidades"),
                                                rs.getBigDecimal("ventas")),
                                desde, hasta);
        }

        private List<DashboardDto.Mozo> mozos(LocalDateTime desde, LocalDateTime hasta) {
                return jdbc.query(SQL_MOZOS,
                                (rs, n) -> new DashboardDto.Mozo(rs.getInt("id_empleado"), rs.getString("nombre"),
                                                rs.getInt("pedidos"), rs.getBigDecimal("ventas")),
                                desde, hasta);
        }

        // ------------------------------------------------------------------ reservas
        private DashboardDto.Reservas reservas(LocalDate hoy) {
                List<DashboardDto.ReservaHoy> lista = jdbc.query(SQL_RESERVAS_HOY,
                                (rs, n) -> new DashboardDto.ReservaHoy(rs.getInt("id_reserva"),
                                                rs.getObject("hora_reserva", LocalTime.class), rs.getInt("numero_mesa"),
                                                rs.getString("zona_mesa"), rs.getString("cliente"),
                                                rs.getInt("cant_personas"),
                                                rs.getString("estado"), rs.getString("canal_reserva")),
                                hoy);
                Integer manana = jdbc.queryForObject(SQL_RESERVAS_MANANA, Integer.class, hoy.plusDays(1));
                return new DashboardDto.Reservas(lista.size(),
                                contar(lista, "Pendiente"), contar(lista, "Confirmada"),
                                contar(lista, "Atendida"), contar(lista, "Cancelada"),
                                manana == null ? 0 : manana, lista);
        }

        private int contar(List<DashboardDto.ReservaHoy> lista, String estado) {
                return (int) lista.stream().filter(r -> estado.equals(r.estado())).count();
        }

        // ------------------------------------------------------------------
        // abastecimiento
        private DashboardDto.Compras compras(LocalDateTime desde, LocalDateTime hasta, LocalDateTime desdeSemana) {
                Pendientes pend = jdbc.queryForObject(SQL_COMPRAS_PENDIENTES,
                                (rs, n) -> new Pendientes(rs.getInt("ordenes"), rs.getBigDecimal("monto")));
                BigDecimal periodo = jdbc.queryForObject(SQL_COMPRAS_PERIODO, BigDecimal.class, desde, hasta);
                List<DashboardDto.OrdenPendiente> ordenes = jdbc.query(SQL_ORDENES_PENDIENTES,
                                (rs, n) -> new DashboardDto.OrdenPendiente(rs.getInt("id_abastecimiento"),
                                                rs.getString("prov_razon_social"),
                                                rs.getObject("fecha", LocalDateTime.class),
                                                rs.getBigDecimal("monto")));

                Map<Integer, BigDecimal> enCamino = new HashMap<>();
                for (Map<String, Object> fila : jdbc.queryForList(SQL_EN_CAMINO)) {
                        enCamino.put(((Number) fila.get("id_insumo")).intValue(),
                                        new BigDecimal(fila.get("cantidad").toString()));
                }

                List<DashboardDto.InsumoBajo> bajos = jdbc.query(SQL_STOCK, (rs, n) -> {
                        BigDecimal stock = rs.getBigDecimal("recibido").subtract(rs.getBigDecimal("consumido"));
                        BigDecimal ritmoDiario = rs.getBigDecimal("consumo_semana")
                                        .divide(BigDecimal.valueOf(7), 6, RoundingMode.HALF_UP);
                        BigDecimal diasCobertura;
                        if (stock.signum() <= 0) {
                                diasCobertura = BigDecimal.ZERO.setScale(1);
                        } else if (ritmoDiario.signum() > 0) {
                                diasCobertura = stock.divide(ritmoDiario, 1, RoundingMode.HALF_UP);
                        } else {
                                diasCobertura = null; // no se consumio en la ultima semana
                        }
                        int idInsumo = rs.getInt("id_insumo");
                        return new DashboardDto.InsumoBajo(idInsumo, rs.getString("nombre_insumo"),
                                        rs.getString("unidad_medida"), stock.setScale(3, RoundingMode.HALF_UP),
                                        ritmoDiario.setScale(3, RoundingMode.HALF_UP), diasCobertura,
                                        enCamino.getOrDefault(idInsumo, BigDecimal.ZERO));
                }, desdeSemana).stream()
                                .filter(i -> i.diasCobertura() != null
                                                && i.diasCobertura().compareTo(DIAS_STOCK_BAJO) < 0)
                                .sorted(Comparator.comparing(DashboardDto.InsumoBajo::diasCobertura))
                                .toList();

                return new DashboardDto.Compras(pend.ordenes(), pend.monto(), periodo, bajos.size(), ordenes,
                                bajos.stream().limit(10).toList());
        }
}