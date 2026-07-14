package com.floristeria.floristeria.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductoComplementarioRequestDTO {

    @NotNull(message = "El producto complementario es obligatorio")
    private Integer complementarioId;

    private Integer sedeId;

    private Integer orden;
}
