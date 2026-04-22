package com.floristeria.floristeria.repository;

import com.floristeria.floristeria.entity.DetallePedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Integer> {

    /**
     * Busca todos los detalles asociados a un pedidoId.
     */
    // CORREGIDO: Pedido_Id
    List<DetallePedido> findByPedido_Id(Integer pedidoId);
}