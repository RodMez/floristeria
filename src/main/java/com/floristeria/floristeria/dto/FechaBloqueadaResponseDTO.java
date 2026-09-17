package com.floristeria.floristeria.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class FechaBloqueadaResponseDTO {

    private Integer id;
    private Integer sedeId;
    private LocalDate fecha;
    private String motivo;
}
