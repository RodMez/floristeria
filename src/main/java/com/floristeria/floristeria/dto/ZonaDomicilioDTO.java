package com.floristeria.floristeria.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

public class ZonaDomicilioDTO {

    @Getter
    @Setter
    public static class ZonaDomicilioRequestDTO {

        @NotNull(message = "El ID de la sede es obligatorio")
        private Integer sedeId;

        @NotBlank(message = "La localidad es obligatoria")
        private String localidad;

        private String barrio;

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        private BigDecimal precio;
    }

    @Getter
    @Setter
    @Builder
    public static class ZonaDomicilioResponseDTO {
        private Integer id;
        private Integer sedeId;
        private String sedeNombre;
        private String localidad;
        private String barrio;
        private BigDecimal precio;
    }
}
