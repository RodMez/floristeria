package com.floristeria.floristeria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProductoRequestDTO {

    @NotBlank
    private String nombre;

    @NotBlank
    private String descripcion;

    @NotBlank
    private String imagenUrl;

    private String sku;

    @NotNull
    @Size(min = 1, message = "Debe seleccionar al menos una categoría")
    private List<Integer> categoriaIds;
}
