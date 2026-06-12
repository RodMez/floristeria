package com.floristeria.floristeria.dto;

import com.floristeria.floristeria.entity.EstadoPedido;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PedidoEstadoUpdateRequestDTO {

    @NotNull(message = "El estado no puede estar vacío")
    private EstadoPedido estado;
}
