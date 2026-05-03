package com.floristeria.floristeria.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PedidoAdminResponseDTO {

    private Integer id;
    private String clienteNombre;
    private String clienteTelefono;
    private BigDecimal total;
    private String estado;
    private LocalDateTime creadoEn;
}
