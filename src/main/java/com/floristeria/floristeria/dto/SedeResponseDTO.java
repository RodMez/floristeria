package com.floristeria.floristeria.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SedeResponseDTO {

    private Integer id;
    private String nombre;
    private String ciudad;
    private String telefonoWhatsapp;
    private String instagramUrl;
    private String facebookUrl;
    private String email;
}
