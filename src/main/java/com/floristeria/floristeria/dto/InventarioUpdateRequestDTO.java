package com.floristeria.floristeria.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventarioUpdateRequestDTO {

    @NotNull(message = "El precio no puede ser nulo")
    @PositiveOrZero(message = "El precio debe ser mayor o igual a cero")
    private BigDecimal precio;

    @NotNull(message = "El stock no puede ser nulo")
    @PositiveOrZero(message = "El stock debe ser mayor o igual a cero")
    private Integer stock;

    @NotNull(message = "El campo disponible no puede ser nulo")
    private Boolean disponible;

    private Integer descuentoPorcentaje;
}
