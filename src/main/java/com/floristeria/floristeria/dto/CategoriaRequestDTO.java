package com.floristeria.floristeria.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoriaRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String tipo;

    private Boolean mostrarEnCatalogo;

    private Integer orden;
}
