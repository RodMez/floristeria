package com.floristeria.floristeria.repository;

import com.floristeria.floristeria.entity.Reseña;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReseñaRepository extends JpaRepository<Reseña, Integer> {

    List<Reseña> findByProducto_IdAndAprobadaTrueAndDeletedAtIsNullOrderByCreadoEnDesc(Integer productoId);

    Optional<Reseña> findByProducto_IdAndCliente_IdAndDeletedAtIsNull(Integer productoId, Integer clienteId);

    @Query("SELECT AVG(r.calificacion) FROM Reseña r WHERE r.producto.id = :productoId AND r.aprobada = true AND r.deletedAt IS NULL")
    Double findAverageRatingByProductoId(Integer productoId);

    @Query("SELECT COUNT(r) FROM Reseña r WHERE r.producto.id = :productoId AND r.aprobada = true AND r.deletedAt IS NULL")
    Integer findCountByProductoId(Integer productoId);

    List<Reseña> findAllByDeletedAtIsNullOrderByCreadoEnDesc();

    List<Reseña> findByAprobadaFalseAndDeletedAtIsNullOrderByCreadoEnDesc();
}
