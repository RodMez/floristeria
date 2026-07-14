package com.floristeria.floristeria.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoCatalogoDetalleDTO {
    private Integer inventarioId;
    private Integer productoId;
    private String nombre;
    private String descripcion;
    private String imagenUrl;
    private String sku;
    private Integer sedeId;
    private String sedeNombre;
    private BigDecimal precioBase;
    private Integer descuentoPorcentaje;
    private BigDecimal precioFinal;
    private Integer stock;
    private Boolean disponible;
    private List<String> categoriasNombres;
    private Double ratingAverage;
    private Integer ratingCount;
    private List<ProductoCatalogoDTO> productosComplementarios;
}
