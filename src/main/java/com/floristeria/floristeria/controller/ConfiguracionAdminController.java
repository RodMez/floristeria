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
                request.getEnviarCopiaMaestro() != null ? request.getEnviarCopiaMaestro() : false
        );
        return ResponseEntity.ok(mapToResponse(config));
    }

    private ConfiguracionTiendaDTO.ResponseDTO mapToResponse(ConfiguracionTienda config) {
        return ConfiguracionTiendaDTO.ResponseDTO.builder()
                .id(config.getId())
                .correoMaestro(config.getCorreoMaestro())
                .enviarCopiaMaestro(config.getEnviarCopiaMaestro())
                .build();
    }
}
