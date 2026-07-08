package com.floristeria.floristeria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReseñaEstadoDTO {

    private Boolean puedeCrear;
    private ReseñaResponseDTO miReseña;
}
