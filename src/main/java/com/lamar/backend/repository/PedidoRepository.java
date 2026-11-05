package com.lamar.backend.repository;

import com.lamar.backend.entity.Pedido;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    Optional<Pedido> findFirstByMesaIdAndEstadoPedido(Integer mesaId, String estadoPedido);
}