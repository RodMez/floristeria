package com.floristeria.floristeria.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ClientePerfilResponseDTO {

    private Integer id;
    private String nombre;
    private String email;
    private String telefono;
}
