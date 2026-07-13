package com.floristeria.floristeria.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DetallePedidoExcelDTO {

    private String pedidoCodigo;
    private String productoNombre;
    private String productoSku;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private String notaPersonalizacion;
}
