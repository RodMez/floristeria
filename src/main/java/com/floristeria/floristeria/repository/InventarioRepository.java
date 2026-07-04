package com.floristeria.floristeria.repository;

import com.floristeria.floristeria.entity.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Integer> {

    /**
     * Busca el inventario de un producto específico en una sede específica.
     */
    // CORREGIDO: Producto_Id y Sede_Id
    Inventario findByProducto_IdAndSede_Id(Integer productoId, Integer sedeId);

    /**
     * Lista todo el inventario disponible (disponible = true y stock > 0) filtrado por sede_id.
     */
    // CORREGIDO: Sede_Id
    List<Inventario> findBySede_IdAndDisponibleTrueAndStockGreaterThan(Integer sedeId, Integer stock);

    /**
     * Lista todo el inventario por sede_id (incluye agotados).
     */
    // CORREGIDO: Sede_Id
    List<Inventario> findBySede_Id(Integer sedeId);

    /**
     * Lista inventarios para múltiples sedes (optimizado para verificación global).
     */
    // CORREGIDO: Sede_Id
    List<Inventario> findBySede_IdIn(List<Integer> sedeIds);

    /**
     * Lista inventarios cuyo producto NO esté soft-deleteado (usa INNER JOIN estricto).
     */
    @Query("SELECT i FROM Inventario i JOIN i.producto p WHERE p.deletedAt IS NULL")
    List<Inventario> findAllActive();

    /**
     * Lista inventarios por sede cuyo producto NO esté soft-deleteado (usa INNER JOIN estricto).
     */
    @Query("SELECT i FROM Inventario i JOIN i.producto p WHERE i.sede.id = :sedeId AND p.deletedAt IS NULL")
    List<Inventario> findActiveBySedeId(@Param("sedeId") Integer sedeId);

    /**
     * Lista inventarios disponibles para Meta Feed: activos globalmente, con stock > 0, disponibles.
     */
    @Query("SELECT i FROM Inventario i JOIN i.producto p " +
           "WHERE i.disponible = true AND i.stock > 0 " +
           "AND p.activoGlobal = true AND p.deletedAt IS NULL")
    List<Inventario> findAvailableForFeed();

    boolean existsBySede_IdAndDisponibleTrue(Integer sedeId);

    boolean existsBySede_IdAndDisponibleTrueAndStockGreaterThan(Integer sedeId, Integer stock);

    @Query("SELECT COUNT(i) > 0 FROM Inventario i WHERE i.sede.id = :sedeId AND i.disponible = true AND i.stock > 0")
    boolean existsAvailableBySedeId(@Param("sedeId") Integer sedeId);

    @Modifying
    @Query("UPDATE Inventario i SET i.deletedAt = :now WHERE i.sede.id = :sedeId")
    void softDeleteBySedeId(@Param("sedeId") Integer sedeId, @Param("now") LocalDateTime now);

    List<Inventario> findByProducto_IdAndDisponibleTrueAndStockGreaterThan(Integer productoId, Integer stock);

    boolean existsByProducto_IdAndDisponibleTrueAndStockGreaterThan(Integer productoId, Integer stock);
}