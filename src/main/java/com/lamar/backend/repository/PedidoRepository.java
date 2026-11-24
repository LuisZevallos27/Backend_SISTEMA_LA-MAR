package com.lamar.backend.repository;

import com.lamar.backend.entity.Pedido;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    Optional<Pedido> findFirstByMesaIdAndEstadoPedido(Integer mesaId, String estadoPedido);

    /** Carga el pedido con su mesa y su empleado en una sola consulta (sin lazy loading). */
    @Query("select p from Pedido p join fetch p.mesa join fetch p.empleado where p.id = :id")
    Optional<Pedido> buscarConMesaYEmpleado(@Param("id") Integer id);

    /** Carga el pedido con sus platos (detalle) en una sola consulta (sin lazy loading). */
    @Query("select distinct p from Pedido p left join fetch p.detalles d left join fetch d.plato where p.id = :id")
    Optional<Pedido> buscarConPlatos(@Param("id") Integer id);

    /** ¿Hubo en esa mesa algún pedido (que no sea anulado) abierto desde ese momento? */
    boolean existsByMesaIdAndFechaHoraGreaterThanEqualAndEstadoPedidoNot(
            Integer mesaId, LocalDateTime desde, String estadoPedido);
}