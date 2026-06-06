package com.floristeria.floristeria.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.floristeria.floristeria.dto.ProductoCatalogoDTO;
import com.floristeria.floristeria.entity.Categoria;
import com.floristeria.floristeria.entity.Inventario;
import com.floristeria.floristeria.entity.Producto;
import com.floristeria.floristeria.repository.InventarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final InventarioRepository inventarioRepository;

    @Transactional(readOnly = true)
    public List<ProductoCatalogoDTO> obtenerCatalogoPorSede(Integer sedeId) {
        // 1. Usamos el nombre exacto del método del repositorio
        return inventarioRepository.findBySede_IdAndDisponibleTrueAndStockGreaterThan(sedeId, 0).stream()
                .map(inv -> {
                    Producto producto = inv.getProducto();
                    List<String> categoriasNombres = producto.getCategorias().stream()
                            .map(Categoria::getNombre)
                            .collect(Collectors.toList());

                    return ProductoCatalogoDTO.builder()
                            .productoId(producto.getId())
                            .nombre(producto.getNombre())
                            .descripcion(producto.getDescripcion())
                            .imagenUrl(producto.getImagenUrl())
                            .categoriasNombres(categoriasNombres)
                            // 2. Convertimos el Double de la entidad al BigDecimal del DTO
                            .precio(inv.getPrecio())
                            .stock(inv.getStock())
                            .disponible(true)
                            .build();
                })
                .collect(Collectors.toList());
    }
}