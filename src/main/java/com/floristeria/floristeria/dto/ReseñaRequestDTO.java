package com.floristeria.floristeria.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReseñaRequestDTO {

    @NotNull
    private Integer productoId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer calificacion;

    private String comentario;
}
