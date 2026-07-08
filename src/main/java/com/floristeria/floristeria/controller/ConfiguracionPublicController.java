package com.floristeria.floristeria.controller;

import com.floristeria.floristeria.dto.ConfiguracionTiendaDTO;
import com.floristeria.floristeria.entity.ConfiguracionTienda;
import com.floristeria.floristeria.service.ConfiguracionTiendaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/configuracion")
@RequiredArgsConstructor
public class ConfiguracionPublicController {

    private final ConfiguracionTiendaService configuracionService;

    @GetMapping
    public ResponseEntity<ConfiguracionTiendaDTO.PublicResponseDTO> obtenerConfiguracionPublica() {
        ConfiguracionTienda config = configuracionService.obtenerConfiguracion();
        return ResponseEntity.ok(ConfiguracionTiendaDTO.PublicResponseDTO.builder()
                .correoMaestro(config.getCorreoMaestro())
                .whatsappGeneral(config.getWhatsappGeneral())
                .instagramUrl(config.getInstagramUrl())
                .facebookUrl(config.getFacebookUrl())
                .tiktokUrl(config.getTiktokUrl())
                .nombreSitio(config.getNombreSitio())
                .tagline(config.getTagline())
                .descripcion(config.getDescripcion())
                .logoUrl(config.getLogoUrl())
                .iconUrl(config.getIconUrl())
                .historia(config.getHistoria())
                .mision(config.getMision())
                .vision(config.getVision())
                .build());
    }
}
