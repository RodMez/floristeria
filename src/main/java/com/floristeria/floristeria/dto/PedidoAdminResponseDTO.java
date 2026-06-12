package com.floristeria.floristeria.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PedidoAdminResponseDTO {

    private Integer id;
    private String clienteNombre;
    private String clienteEmail;
    private String clienteTelefono;
    private String direccionAlias;
    private String direccionCompleta;
    private String direccionCiudad;
    private BigDecimal total;
    private String estado;
    private String transaccionId;
    private LocalDateTime creadoEn;
}
