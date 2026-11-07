package com.lamar.backend.service;

import com.lamar.backend.dto.PedidoDto;
import com.lamar.backend.dto.PedidoRequests;
import com.lamar.backend.entity.*;
import com.lamar.backend.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private static final BigDecimal IGV = new BigDecimal("0.18");
    private static final String ABIERTO = "Abierto";
    private static final String CERRADO = "Cerrado";
    private static final String ANULADO = "Anulado";

    private final PedidoRepository pedidoRepository;
    private final MesaRepository mesaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ClienteRepository clienteRepository;
    private final PlatoRepository platoRepository;
    private final ReservaRepository reservaRepository;

    @Transactional
    public PedidoDto abrir(Integer idEmpleado, PedidoRequests.Abrir req) {
        Mesa mesa = mesaRepository.findById(req.idMesa())
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Mesa no encontrada"));
        if ("Ocupado".equals(mesa.getEstado())
                || pedidoRepository.findFirstByMesaIdAndEstadoPedido(mesa.getId(), ABIERTO).isPresent()) {
            throw error(HttpStatus.CONFLICT, "La mesa ya tiene un pedido abierto");
        }
        Empleado empleado = empleadoRepository.findById(idEmpleado)
                .orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "Empleado no valido"));

        Pedido p = new Pedido();
        p.setMesa(mesa);
        p.setEmpleado(empleado);
        p.setFechaHora(LocalDateTime.now());
        p.setTipoComprobante("Boleta");
        p.setMontoTotal(BigDecimal.ZERO);
        p.setEstadoPedido(ABIERTO);
        if (req.dniCliente() != null && !req.dniCliente().isBlank()) {
            p.setCliente(buscarCliente(req.dniCliente()));
        }
        reservaRepository.findFirstByMesaIdAndFechaAndEstadoInOrderByHoraAsc(
                mesa.getId(), LocalDate.now(), List.of("Pendiente", "Confirmada"))
                .ifPresent(r -> {
                    r.setEstado("Atendida");
                    if (p.getCliente() == null) {
                        p.setCliente(r.getCliente());
                    }
                });

        mesa.setEstado("Ocupado");
        pedidoRepository.save(p);
        return toDto(p);
    }

    @Transactional(readOnly = true)
    public PedidoDto obtener(Integer idPedido) {
        return toDto(pedidoRepository.findById(idPedido)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Pedido no encontrado")));
    }

    @Transactional(readOnly = true)
    public PedidoDto obtenerAbiertoPorMesa(Integer idMesa) {
        return toDto(pedidoRepository.findFirstByMesaIdAndEstadoPedido(idMesa, ABIERTO)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "La mesa no tiene un pedido abierto")));
    }

    @Transactional
    public PedidoDto agregarItem(Integer idPedido, PedidoRequests.AgregarItem req) {
        Pedido p = pedidoAbierto(idPedido);
        Plato plato = platoRepository.findById(req.idPlato())
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Plato no encontrado"));

        DetallePedido d = buscarDetalle(p, plato.getId());
        if (d == null) {
            d = new DetallePedido();
            d.getId().setIdPedido(p.getId());
            d.getId().setIdPlato(plato.getId());
            d.setPedido(p);
            d.setPlato(plato);
            d.setCantidad(req.cantidad());
            p.getDetalles().add(d);
        } else {
            d.setCantidad(d.getCantidad() + req.cantidad());
        }
        d.setSubtotal(plato.getPrecio().multiply(BigDecimal.valueOf(d.getCantidad())));
        recalcular(p);
        return toDto(p);
    }

    @Transactional
    public PedidoDto cambiarCantidad(Integer idPedido, Integer idPlato, PedidoRequests.CambiarCantidad req) {
        Pedido p = pedidoAbierto(idPedido);
        DetallePedido d = buscarDetalle(p, idPlato);
        if (d == null) {
            throw error(HttpStatus.NOT_FOUND, "El plato no esta en el pedido");
        }
        d.setCantidad(req.cantidad());
        d.setSubtotal(d.getPlato().getPrecio().multiply(BigDecimal.valueOf(req.cantidad())));
        recalcular(p);
        return toDto(p);
    }

    @Transactional
    public PedidoDto quitarItem(Integer idPedido, Integer idPlato) {
        Pedido p = pedidoAbierto(idPedido);
        boolean quitado = p.getDetalles().removeIf(d -> d.getPlato().getId().equals(idPlato));
        if (!quitado) {
            throw error(HttpStatus.NOT_FOUND, "El plato no esta en el pedido");
        }
        recalcular(p);
        return toDto(p);
    }

    @Transactional
    public PedidoDto asignarCliente(Integer idPedido, PedidoRequests.AsignarCliente req) {
        Pedido p = pedidoAbierto(idPedido);
        p.setCliente(buscarCliente(req.dni()));
        return toDto(p);
    }

    @Transactional
    public PedidoDto cerrar(Integer idPedido, PedidoRequests.Cerrar req) {
        Pedido p = pedidoAbierto(idPedido);
        if (p.getDetalles().isEmpty()) {
            throw error(HttpStatus.BAD_REQUEST, "No se puede cerrar un pedido sin platos");
        }
        if (req != null && req.tipoComprobante() != null && !req.tipoComprobante().isBlank()) {
            String tipo = req.tipoComprobante().trim();
            if (!tipo.equals("Boleta") && !tipo.equals("Factura")) {
                throw error(HttpStatus.BAD_REQUEST, "Tipo de comprobante invalido (Boleta o Factura)");
            }
            p.setTipoComprobante(tipo);
        }
        recalcular(p);
        p.setEstadoPedido(CERRADO);
        p.getMesa().setEstado("Disponible");
        return toDto(p);
    }

    @Transactional
    public PedidoDto anular(Integer idPedido) {
        Pedido p = pedidoAbierto(idPedido);
        p.setEstadoPedido(ANULADO);
        p.getMesa().setEstado("Disponible");
        return toDto(p);
    }

    // ---------- auxiliares ----------

    private Pedido pedidoAbierto(Integer idPedido) {
        Pedido p = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        if (!ABIERTO.equals(p.getEstadoPedido())) {
            throw error(HttpStatus.CONFLICT, "El pedido no esta abierto");
        }
        return p;
    }

    private Cliente buscarCliente(String dni) {
        return clienteRepository.findByDni(dni.trim())
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Cliente no registrado"));
    }

    private DetallePedido buscarDetalle(Pedido p, Integer idPlato) {
        return p.getDetalles().stream()
                .filter(d -> d.getPlato().getId().equals(idPlato))
                .findFirst()
                .orElse(null);
    }

    private void recalcular(Pedido p) {
        BigDecimal base = p.getDetalles().stream()
                .map(DetallePedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal igv = base.multiply(IGV).setScale(2, RoundingMode.HALF_UP);
        p.setMontoTotal(base.add(igv));
    }

    private PedidoDto toDto(Pedido p) {
        List<PedidoDto.Item> items = p.getDetalles().stream()
                .sorted(Comparator.comparing((DetallePedido d) -> d.getPlato().getNombre()))
                .map(d -> new PedidoDto.Item(d.getPlato().getId(), d.getPlato().getNombre(),
                        d.getPlato().getPrecio(), d.getCantidad(), d.getSubtotal()))
                .toList();
        BigDecimal base = items.stream()
                .map(PedidoDto.Item::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal igv = base.multiply(IGV).setScale(2, RoundingMode.HALF_UP);
        Cliente c = p.getCliente();
        return new PedidoDto(
                p.getId(), p.getMesa().getId(), p.getMesa().getNumero(), p.getMesa().getZona(),
                p.getEstadoPedido(), p.getFechaHora(), p.getTipoComprobante(),
                p.getEmpleado().getNombres() + " " + p.getEmpleado().getApellidos(),
                c == null ? null : c.getDni(),
                c == null ? null : c.getNombres() + " " + c.getApellidos(),
                items, base, igv, base.add(igv));
    }

    private ResponseStatusException error(HttpStatus status, String mensaje) {
        return new ResponseStatusException(status, mensaje);
    }
}