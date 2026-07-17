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
public class ProductoShowcaseDTO {
    private Integer productoId;
    private String nombre;
    private String descripcion;
    private String imagenUrl;
    private String sku;
    private List<String> categoriasNombres;
    private Double ratingAverage;
    private Integer ratingCount;
    private List<VarianteDTO> variantes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VarianteDTO {
        private Integer sedeId;
        private String sedeNombre;
        private String ciudad;
        private BigDecimal precio;
        private Integer descuentoPorcentaje;
        private BigDecimal precioFinal;
        private Integer stock;
    }
}
