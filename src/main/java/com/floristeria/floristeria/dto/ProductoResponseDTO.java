package com.floristeria.floristeria.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ProductoResponseDTO {

    private Integer id;
    private String nombre;
    private String descripcion;
    private String imagenUrl;
    private String sku;
    private List<CategoriaInfo> categorias;

    @Getter
    @Setter
    @Builder
    public static class CategoriaInfo {
        private Integer id;
        private String nombre;
    }
}
