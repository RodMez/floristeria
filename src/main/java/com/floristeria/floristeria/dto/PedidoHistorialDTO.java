package com.floristeria.floristeria.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PedidoHistorialDTO {

    private Integer id;
    private BigDecimal total;
    private String estado;
    private LocalDateTime creadoEn;
    private String referenciaPago;
}
