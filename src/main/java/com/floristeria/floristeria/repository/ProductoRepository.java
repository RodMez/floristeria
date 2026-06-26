package com.floristeria.floristeria.repository;

import com.floristeria.floristeria.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    /**
     * Busca productos activos globalmente (activo_global = true).
     * Utilizado para el catálogo maestro y sincronización entre sedes.
     */
    List<Producto> findByActivoGlobalTrue();

    /**
     * Busca productos por nombre (consulta parcial).
     */
    List<Producto> findByNombreContainingIgnoreCase(String nombre);

    /**
     * Busca productos activos globalmente por categoría.
     */
    List<Producto> findByActivoGlobalTrueAndCategorias_Id(Integer categoriaId);

    /**
     * Verifica si existe un producto con el nombre especificado.
     */
    boolean existsByNombre(String nombre);

    /**
     * Busca productos por múltiples IDs (utilizado para verificación masiva).
     */
    List<Producto> findByIdIn(List<Integer> ids);

    /**
     * Busca un producto por su SKU (único).
     */
    java.util.Optional<Producto> findBySku(String sku);
}