package com.floristeria.floristeria.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProductoComplementarioResponseDTO {

    private Integer id;
    private Integer complementarioId;
    private String complementarioNombre;
    private String complementarioImagenUrl;
    private Integer sedeId;
    private String sedeNombre;
    private Integer orden;
}
