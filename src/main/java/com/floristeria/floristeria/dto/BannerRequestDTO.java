package com.floristeria.floristeria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BannerRequestDTO {

    private Integer sedeId;

    @NotBlank(message = "La ubicacion es obligatoria")
    private String ubicacion;

    @Size(max = 200, message = "El título no puede superar los 200 caracteres")
    private String titulo;

    @Size(max = 500, message = "El texto no puede superar los 500 caracteres")
    private String texto;

    @NotBlank(message = "La URL de la imagen es obligatoria")
    private String imagenUrl;

    @Size(max = 500, message = "El enlace no puede superar los 500 caracteres")
    private String enlaceUrl;
    private Integer orden;
    private Boolean activo;
}
