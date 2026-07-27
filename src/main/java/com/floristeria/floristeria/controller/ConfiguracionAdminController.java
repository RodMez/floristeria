package com.floristeria.floristeria.controller;

import com.floristeria.floristeria.dto.ConfiguracionTiendaDTO;
import com.floristeria.floristeria.entity.ConfiguracionTienda;
import com.floristeria.floristeria.service.ConfiguracionTiendaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/superadmin/configuracion")
@RequiredArgsConstructor
public class ConfiguracionAdminController {

    private final ConfiguracionTiendaService configuracionService;

    @GetMapping
    public ResponseEntity<ConfiguracionTiendaDTO.ResponseDTO> obtenerConfiguracion() {
        ConfiguracionTienda config = configuracionService.obtenerConfiguracion();
        return ResponseEntity.ok(mapToResponse(config));
    }

    @PutMapping
    public ResponseEntity<ConfiguracionTiendaDTO.ResponseDTO> actualizarConfiguracion(
            @Valid @RequestBody ConfiguracionTiendaDTO.RequestDTO request) {
        ConfiguracionTienda config = configuracionService.actualizarConfiguracion(
                request.getCorreoMaestro(),
                request.getEnviarCopiaMaestro(),
                request.getWhatsappGeneral(),
                request.getInstagramUrl(),
                request.getFacebookUrl(),
                request.getTiktokUrl(),
                request.getImagenHeroUrl(),
                request.getImagenBannerUrl(),
                request.getNombreSitio(),
                request.getTagline(),
                request.getDescripcion(),
                request.getLogoUrl(),
                request.getIconUrl(),
                request.getHistoria(),
                request.getMision(),
                request.getVision(),
                request.getShowcaseBadge(),
                request.getShowcaseTitulo(),
                request.getShowcaseSubtitulo(),
                request.getNit(),
                request.getRazonSocial(),
                request.getRepresentanteLegal(),
                request.getDomicilioComercial(),
                request.getCorreoHabeasData()
        );
        return ResponseEntity.ok(mapToResponse(config));
    }

    private ConfiguracionTiendaDTO.ResponseDTO mapToResponse(ConfiguracionTienda config) {
        return ConfiguracionTiendaDTO.ResponseDTO.builder()
                .id(config.getId())
                .correoMaestro(config.getCorreoMaestro())
                .enviarCopiaMaestro(config.getEnviarCopiaMaestro())
                .whatsappGeneral(config.getWhatsappGeneral())
                .instagramUrl(config.getInstagramUrl())
                .facebookUrl(config.getFacebookUrl())
                .tiktokUrl(config.getTiktokUrl())
                .imagenHeroUrl(config.getImagenHeroUrl())
                .imagenBannerUrl(config.getImagenBannerUrl())
                .nombreSitio(config.getNombreSitio())
                .tagline(config.getTagline())
                .descripcion(config.getDescripcion())
                .logoUrl(config.getLogoUrl())
                .iconUrl(config.getIconUrl())
                .historia(config.getHistoria())
                .mision(config.getMision())
                .vision(config.getVision())
                .showcaseBadge(config.getShowcaseBadge())
                .showcaseTitulo(config.getShowcaseTitulo())
                .showcaseSubtitulo(config.getShowcaseSubtitulo())
                .nit(config.getNit())
                .razonSocial(config.getRazonSocial())
                .representanteLegal(config.getRepresentanteLegal())
                .domicilioComercial(config.getDomicilioComercial())
                .correoHabeasData(config.getCorreoHabeasData())
                .politicaVersionActual(config.getPoliticaVersionActual())
                .tycVersionActual(config.getTycVersionActual())
                .build();
    }
}
