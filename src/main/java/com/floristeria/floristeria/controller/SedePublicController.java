package com.floristeria.floristeria.controller;

import com.floristeria.floristeria.dto.EntregaConfigDTO;
import com.floristeria.floristeria.dto.SedeResponseDTO;
import com.floristeria.floristeria.dto.SlotEntregaDTO;
import com.floristeria.floristeria.service.EntregaService;
import com.floristeria.floristeria.service.SedeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sedes")
@RequiredArgsConstructor
public class SedePublicController {

    private final SedeService sedeService;
    private final EntregaService entregaService;

    @GetMapping
    public ResponseEntity<List<SedeResponseDTO>> listarTodas() {
        return ResponseEntity.ok(sedeService.listarTodas());
    }

    @GetMapping("/{id}/entrega-config")
    public ResponseEntity<EntregaConfigDTO> obtenerEntregaConfig(@PathVariable Integer id) {
        return ResponseEntity.ok(entregaService.obtenerConfig(id));
    }

    @GetMapping("/{id}/slots")
    public ResponseEntity<List<SlotEntregaDTO>> listarSlots(
            @PathVariable Integer id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(entregaService.listarSlots(id, fecha));
    }
}
