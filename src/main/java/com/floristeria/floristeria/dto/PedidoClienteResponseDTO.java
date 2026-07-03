package com.floristeria.floristeria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoClienteResponseDTO {
    private String pedidoId;
    private BigDecimal total;
    private String estado;
    private String referenciaWompi;
    private Long montoEnCentavos;
    private String firmaIntegridad;
    private String publicKeyWompi;
}