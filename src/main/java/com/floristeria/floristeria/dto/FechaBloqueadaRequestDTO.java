package com.floristeria.floristeria.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class FechaBloqueadaRequestDTO {

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    private String motivo;
}
