package com.floristeria.floristeria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteAuthResponseDTO {

    private String token;
    private Integer clienteId;
    private String nombre;
    private String email;
    private String rol;
}