package com.floristeria.floristeria.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ConfiguracionTiendaDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RequestDTO {
        @Email(message = "El correo maestro debe ser un correo válido")
        private String correoMaestro;

        private Boolean enviarCopiaMaestro;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResponseDTO {
        private Integer id;
        private String correoMaestro;
        private Boolean enviarCopiaMaestro;
    }
}
