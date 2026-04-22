package com.floristeria.floristeria.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetallePedidoRequestDTO {
    private Integer productoId;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private String notaPersonalizacion;
}