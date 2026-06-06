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
public class ProductoCatalogoDTO {
    private Integer productoId;
    private String nombre;
    private String descripcion;
    private String imagenUrl;
    private List<String> categoriasNombres;
    private BigDecimal precio;
    private Integer stock;
    private Boolean disponible;
}