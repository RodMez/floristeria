package com.floristeria.floristeria.repository;

import com.floristeria.floristeria.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    /**
     * Lista todos los pedidos de una sede específica, ordenados por fecha de creación descendente.
     * Utilizado para el panel del Admin local.
     */
    // CORREGIDO: Sede_Id
    List<Pedido> findBySede_IdOrderByCreadoEnDesc(Integer sedeId);

    /**
     * Lista pedidos por sede_id y estado.
     */
    // CORREGIDO: Sede_Id
    List<Pedido> findBySede_IdAndEstado(Integer sedeId, String estado);
}