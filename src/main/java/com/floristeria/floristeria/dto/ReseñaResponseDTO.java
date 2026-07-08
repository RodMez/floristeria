package com.floristeria.floristeria.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReseñaResponseDTO {

    private Integer id;
    private Integer productoId;
    private Integer clienteId;
    private String clienteNombre;
    private Integer calificacion;
    private String comentario;
    private Boolean aprobada;
    private LocalDateTime creadoEn;
}
