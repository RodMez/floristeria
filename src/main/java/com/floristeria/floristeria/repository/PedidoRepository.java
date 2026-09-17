package com.floristeria.floristeria.repository;

import com.floristeria.floristeria.entity.EstadoPedido;
import com.floristeria.floristeria.entity.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    Optional<Pedido> findByCodigo(String codigo);

    @EntityGraph(attributePaths = {"sede", "cliente", "direccion", "direccion.zonaDomicilio", "detalles", "detalles.producto"})
    @Query("SELECT p FROM Pedido p WHERE p.codigo = :codigo")
    Optional<Pedido> findByCodigoWithFetch(@Param("codigo") String codigo);

    boolean existsBySede_IdAndEstadoNotIn(Integer sedeId, List<EstadoPedido> estados);

    List<Pedido> findBySede_IdAndFechaEntregaBetween(Integer sedeId, java.time.LocalDate desde, java.time.LocalDate hasta);

    @Query("SELECT CASE WHEN COUNT(dp) > 0 THEN true ELSE false END FROM Pedido p JOIN p.detalles dp WHERE p.cliente.id = :clienteId AND dp.producto.id = :productoId AND p.estado = :estado AND p.deletedAt IS NULL")
    boolean existsByClienteIdAndProductoIdAndEstado(@Param("clienteId") Integer clienteId, @Param("productoId") Integer productoId, @Param("estado") EstadoPedido estado);

    @Query(value = "SELECT DISTINCT p.* FROM pedidos p " +
           "WHERE p.deleted_at IS NULL " +
           "ORDER BY p.creado_en DESC",
           nativeQuery = true)
    List<Pedido> findAllPedidosParaExportar();
}
