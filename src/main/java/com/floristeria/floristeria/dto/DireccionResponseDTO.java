package com.floristeria.floristeria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DireccionResponseDTO {
    private Integer id;
    private String alias;
    private String direccion;
    private String ciudad;
    private String detalles;
}