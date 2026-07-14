package com.floristeria.floristeria.repository;

import com.floristeria.floristeria.entity.ProductoComplementario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ProductoComplementarioRepository extends JpaRepository<ProductoComplementario, Integer> {

    @Query("SELECT pc FROM ProductoComplementario pc " +
           "WHERE pc.producto.id = :productoId " +
           "AND (pc.sede.id = :sedeId OR pc.sede IS NULL) " +
           "ORDER BY pc.orden ASC")
    List<ProductoComplementario> findByProductoIdAndSede(@Param("productoId") Integer productoId,
                                                          @Param("sedeId") Integer sedeId);

    List<ProductoComplementario> findByProductoIdOrderByOrdenAsc(Integer productoId);

    boolean existsByProductoIdAndComplementarioId(Integer productoId, Integer complementarioId);

    void deleteByProductoIdAndComplementarioId(Integer productoId, Integer complementarioId);

    @Query("SELECT pc FROM ProductoComplementario pc " +
           "WHERE (pc.sede.id = :sedeId OR pc.sede IS NULL) " +
           "ORDER BY pc.orden ASC")
    List<ProductoComplementario> findBySede(@Param("sedeId") Integer sedeId);

    @Query("SELECT DISTINCT pc.complementario.id FROM ProductoComplementario pc")
    Set<Integer> findAllComplementarioIds();

    @Query("SELECT DISTINCT pc.complementario.id FROM ProductoComplementario pc " +
           "WHERE pc.sede IS NOT NULL AND pc.sede.id != :sedeId")
    Set<Integer> findComplementarioIdsForOtherSedes(@Param("sedeId") Integer sedeId);
}
