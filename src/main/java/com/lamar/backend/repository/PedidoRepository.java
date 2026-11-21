package com.lamar.backend.repository;

import com.lamar.backend.entity.Pedido;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    Optional<Pedido> findFirstByMesaIdAndEstadoPedido(Integer mesaId, String estadoPedido);

    /**
     * Carga el pedido con su mesa y su empleado en una sola consulta (sin lazy
     * loading).
     */
    @Query("select p from Pedido p join fetch p.mesa join fetch p.empleado where p.id = :id")
    Optional<Pedido> buscarConMesaYEmpleado(@Param("id") Integer id);
}