package com.floristeria.floristeria.repository;

import com.floristeria.floristeria.entity.EstadoPedido;
import com.floristeria.floristeria.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    List<Pedido> findBySede_IdOrderByCreadoEnDesc(Integer sedeId);

    List<Pedido> findBySede_IdAndEstado(Integer sedeId, EstadoPedido estado);

    List<Pedido> findBySede_Id(Integer sedeId);

    Optional<Pedido> findByReferenciaPago(String referenciaPago);

    List<Pedido> findByCliente_IdOrderByCreadoEnDesc(Integer clienteId);
}
