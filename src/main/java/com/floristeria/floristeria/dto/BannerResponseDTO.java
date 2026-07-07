package com.floristeria.floristeria.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter @Builder
public class BannerResponseDTO {
    private Integer id;
    private Integer sedeId;
    private String ubicacion;
    private String titulo;
    private String texto;
    private String imagenUrl;
    private String enlaceUrl;
    private Integer orden;
    private Boolean activo;
    private Instant creadoEn;
    private Instant actualizadoEn;
}
