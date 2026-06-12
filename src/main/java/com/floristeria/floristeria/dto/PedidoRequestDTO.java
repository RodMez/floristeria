package com.floristeria.floristeria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoRequestDTO {
    private Integer sedeId;
    private Integer clienteId;
    private Integer direccionId;
    private String notasEntrega;
    private List<DetallePedidoRequestDTO> detalles;
}
