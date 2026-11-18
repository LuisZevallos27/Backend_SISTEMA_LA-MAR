package com.lamar.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record DashboardDto(
                LocalDateTime generadoEn,
                int dias,
                Hoy hoy,
                Ocupacion ocupacion,
                List<VentaDia> ventasPorDia,
                List<VentaHora> ventasPorHora,
                List<PlatoTop> topPlatos,
                List<Categoria> categorias,
                List<Mozo> mozos,
                List<MesaMapa> mesas,
                Reservas reservas,
                Compras compras) {

        public record Hoy(
                        BigDecimal ventas,
                        BigDecimal ventasAyerMismaHora,
                        BigDecimal variacionPct, // null si ayer no hubo ventas con las que comparar
                        int pedidosCerrados,
                        int pedidosAnulados,
                        BigDecimal ticketPromedio,
                        int pedidosAbiertos,
                        BigDecimal porCobrar) {
        }

        public record Ocupacion(int total, int disponibles, int reservadas, int ocupadas, int porcentaje) {
        }

        public record VentaDia(LocalDate dia, int pedidos, BigDecimal ventas) {
        }

        public record VentaHora(int hora, int pedidos, BigDecimal ventas, BigDecimal promedio) {
        }

        public record PlatoTop(Integer id, String nombre, String categoria, int unidades, BigDecimal ventas) {
        }

        public record Categoria(String nombre, int unidades, BigDecimal ventas) {
        }

        public record Mozo(Integer id, String nombre, int pedidos, BigDecimal ventas) {
        }

        public record MesaMapa(Integer id, Integer numero, String zona, Integer capacidad, String estado,
                        Integer idPedido, BigDecimal total, Integer minutosAbierta) {
        }

        public record Reservas(int total, int pendientes, int confirmadas, int atendidas, int canceladas,
                        int mananaActivas, List<ReservaHoy> lista) {
        }

        public record ReservaHoy(Integer id, LocalTime hora, Integer numeroMesa, String zona, String cliente,
                        Integer personas, String estado, String canal) {
        }

        public record Compras(int ordenesPendientes, BigDecimal montoPendiente, BigDecimal comprasPeriodo,
                        int insumosBajosTotal, List<OrdenPendiente> pendientes, List<InsumoBajo> insumosBajos) {
        }

        public record OrdenPendiente(Integer id, String proveedor, LocalDateTime fecha, BigDecimal monto) {
        }

        public record InsumoBajo(Integer id, String nombre, String unidad, BigDecimal stock,
                        BigDecimal consumoDiario, BigDecimal diasCobertura, BigDecimal enCamino) {
        }
}