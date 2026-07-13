package com.floristeria.floristeria.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductoInventarioExcelDTO {

    private Integer productoId;
    private String sku;
    private String nombre;
    private String descripcion;
    private String categorias;
    private Boolean activoGlobal;
    private LocalDateTime creadoEn;

    private String sedeNombre;
    private BigDecimal precio;
    private Integer stock;
    private Boolean disponible;
    private Integer descuentoPorcentaje;
    private BigDecimal precioFinal;
}
