package com.floristeria.floristeria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DireccionRequestDTO {

    @NotBlank(message = "El alias es obligatorio")
    @Size(max = 50, message = "El alias no puede exceder 50 caracteres")
    private String alias;

    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 255, message = "La dirección no puede exceder 255 caracteres")
    private String direccion;

    @NotBlank(message = "La ciudad es obligatoria")
    @Size(max = 100, message = "La ciudad no puede exceder 100 caracteres")
    private String ciudad;

    @Size(max = 500, message = "Los detalles no pueden exceder 500 caracteres")
    private String detalles;

    @NotNull(message = "El ID de la zona de domicilio es obligatorio")
    private Integer zonaDomicilioId;
}