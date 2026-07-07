package com.floristeria.floristeria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BannerRequestDTO {

    private Integer sedeId;

    @NotBlank(message = "La ubicacion es obligatoria")
    private String ubicacion;

    private String titulo;
    private String texto;

    @NotBlank(message = "La URL de la imagen es obligatoria")
    private String imagenUrl;

    private String enlaceUrl;
    private Integer orden;
    private Boolean activo;
}
