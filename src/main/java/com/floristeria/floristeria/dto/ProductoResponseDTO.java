package com.floristeria.floristeria.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProductoResponseDTO {

    private Integer id;
    private String nombre;
    private String descripcion;
    private String imagenUrl;
    private Integer categoriaId;
    private String categoriaNombre;
}
