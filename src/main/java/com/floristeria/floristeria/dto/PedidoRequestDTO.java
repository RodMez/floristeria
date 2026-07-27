package com.floristeria.floristeria.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
    @NotNull(message = "El ID de la sede es obligatorio")
    private Integer sedeId;

    @NotNull(message = "El ID del cliente es obligatorio")
    private Integer clienteId;

    @NotNull(message = "El ID de la dirección es obligatorio")
    private Integer direccionId;

    private String notasEntrega;

    @NotEmpty(message = "Debe haber al menos un producto en el pedido")
    @Valid
    private List<DetallePedidoRequestDTO> detalles;
}
