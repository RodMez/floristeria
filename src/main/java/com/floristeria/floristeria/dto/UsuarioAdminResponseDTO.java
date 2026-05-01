package com.floristeria.floristeria.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UsuarioAdminResponseDTO {

    private Integer id;
    private String nombre;
    private String email;
    private String rol;
    private Integer sedeId;
    private String sedeNombre;
}
