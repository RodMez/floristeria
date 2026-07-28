package com.floristeria.floristeria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
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

        @Size(max = 20, message = "El WhatsApp no puede superar los 20 caracteres")
        private String whatsappGeneral;

        @Size(max = 500, message = "La URL de Instagram no puede superar los 500 caracteres")
        private String instagramUrl;

        @Size(max = 500, message = "La URL de Facebook no puede superar los 500 caracteres")
        private String facebookUrl;

        @Size(max = 500, message = "La URL de TikTok no puede superar los 500 caracteres")
        private String tiktokUrl;

        private String imagenHeroUrl;

        private String imagenBannerUrl;

        @Size(max = 100, message = "El nombre del sitio no puede superar los 100 caracteres")
        private String nombreSitio;

        @Size(max = 150, message = "El tagline no puede superar los 150 caracteres")
        private String tagline;

        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        private String descripcion;

        @Size(max = 500, message = "La URL del logo no puede superar los 500 caracteres")
        private String logoUrl;

        @Size(max = 500, message = "La URL del ícono no puede superar los 500 caracteres")
        private String iconUrl;

        @Size(max = 10000, message = "La historia no puede superar los 10000 caracteres")
        private String historia;

        @Size(max = 10000, message = "La misión no puede superar los 10000 caracteres")
        private String mision;

        @Size(max = 10000, message = "La visión no puede superar los 10000 caracteres")
        private String vision;

        @Size(max = 100, message = "El badge del showcase no puede superar los 100 caracteres")
        private String showcaseBadge;

        @Size(max = 200, message = "El título del showcase no puede superar los 200 caracteres")
        private String showcaseTitulo;

        @Size(max = 500, message = "El subtítulo del showcase no puede superar los 500 caracteres")
        private String showcaseSubtitulo;

        @Size(max = 30, message = "El NIT no puede superar los 30 caracteres")
        private String nit;

        @Size(max = 200, message = "La razón social no puede superar los 200 caracteres")
        private String razonSocial;

        @Size(max = 150, message = "El representante legal no puede superar los 150 caracteres")
        private String representanteLegal;

        @Size(max = 300, message = "El domicilio comercial no puede superar los 300 caracteres")
        private String domicilioComercial;

        @Email(message = "El correo de Habeas Data debe ser válido")
        @Size(max = 150, message = "El correo de Habeas Data no puede superar los 150 caracteres")
        private String correoHabeasData;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResponseDTO {
        private Integer id;
        private String correoMaestro;
        private Boolean enviarCopiaMaestro;
        private String whatsappGeneral;
        private String instagramUrl;
        private String facebookUrl;
        private String tiktokUrl;
        private String imagenHeroUrl;
        private String imagenBannerUrl;
        private String nombreSitio;
        private String tagline;
        private String descripcion;
        private String logoUrl;
        private String iconUrl;
        private String historia;
        private String mision;
        private String vision;
        private String showcaseBadge;
        private String showcaseTitulo;
        private String showcaseSubtitulo;
        private String nit;
        private String razonSocial;
        private String representanteLegal;
        private String domicilioComercial;
        private String correoHabeasData;
        private String politicaVersionActual;
        private String tycVersionActual;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PublicResponseDTO {
        private String correoMaestro;
        private String whatsappGeneral;
        private String instagramUrl;
        private String facebookUrl;
        private String tiktokUrl;
        private String nombreSitio;
        private String tagline;
        private String descripcion;
        private String logoUrl;
        private String iconUrl;
        private String historia;
        private String mision;
        private String vision;
        private String showcaseBadge;
        private String showcaseTitulo;
        private String showcaseSubtitulo;
        private String correoHabeasData;
        private String politicaVersionActual;
        private String tycVersionActual;
    }
}
