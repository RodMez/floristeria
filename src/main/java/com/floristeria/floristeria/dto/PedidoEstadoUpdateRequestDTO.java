package com.floristeria.floristeria.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PedidoEstadoUpdateRequestDTO {

    @NotBlank(message = "El estado no puede estar vacío")
    private String estado;
}
