package com.floristeria.floristeria.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CategoriaResponseDTO {

    private Integer id;
    private String nombre;
}
