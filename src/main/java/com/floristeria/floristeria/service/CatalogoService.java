package com.floristeria.floristeria.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.floristeria.floristeria.dto.ProductoCatalogoDTO;
import com.floristeria.floristeria.entity.Categoria;
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

                    Integer descuento = inv.getDescuentoPorcentaje();
                    BigDecimal precioConDescuento;

                    if (descuento != null && descuento > 0) {
                        BigDecimal descuentoAmount = inv.getPrecio()
                                .multiply(new BigDecimal(descuento))
                                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                        precioConDescuento = inv.getPrecio().subtract(descuentoAmount);
                    } else {
                        precioConDescuento = inv.getPrecio();
                    }

                    return ProductoCatalogoDTO.builder()
                            .productoId(producto.getId())
                            .nombre(producto.getNombre())
                            .descripcion(producto.getDescripcion())
                            .imagenUrl(producto.getImagenUrl())
                            .sku(producto.getSku())
                            .categoriasNombres(categoriasNombres)
                            .precio(inv.getPrecio())
                            .descuentoPorcentaje(descuento)
                            .precioConDescuento(precioConDescuento)
                            .stock(inv.getStock())
                            .disponible(true)
                            .build();
                })
                .collect(Collectors.toList());
    }
}