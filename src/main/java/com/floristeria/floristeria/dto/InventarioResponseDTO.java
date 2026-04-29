package com.floristeria.floristeria.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class InventarioResponseDTO {
    private Integer id;
    private String productoNombre;
    private String sedeNombre;
    private BigDecimal precio;
    private Integer stock;
    private Boolean disponible;
}
