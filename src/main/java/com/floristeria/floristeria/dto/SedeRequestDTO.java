package com.floristeria.floristeria.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SedeRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "La ciudad es obligatoria")
    private String ciudad;

    @NotBlank(message = "El teléfono de WhatsApp es obligatorio")
    private String telefonoWhatsapp;

    private String instagramUrl;

    private String facebookUrl;

    private String tiktokUrl;

    private String email;
}
