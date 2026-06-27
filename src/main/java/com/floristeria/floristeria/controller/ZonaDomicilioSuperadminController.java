package com.floristeria.floristeria.controller;

import com.floristeria.floristeria.dto.ZonaDomicilioDTO;
import com.floristeria.floristeria.service.ZonaDomicilioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/superadmin/zonas-domicilio")
@RequiredArgsConstructor
public class ZonaDomicilioSuperadminController {

    private final ZonaDomicilioService zonaDomicilioService;

    @GetMapping
    public ResponseEntity<List<ZonaDomicilioDTO.ZonaDomicilioResponseDTO>> listarTodas() {
        return ResponseEntity.ok(zonaDomicilioService.listarTodas());
    }

    @GetMapping("/sede/{sedeId}")
    public ResponseEntity<List<ZonaDomicilioDTO.ZonaDomicilioResponseDTO>> listarPorSede(
            @PathVariable Integer sedeId) {
        return ResponseEntity.ok(zonaDomicilioService.listarPorSede(sedeId));
    }

    @PostMapping
    public ResponseEntity<ZonaDomicilioDTO.ZonaDomicilioResponseDTO> crear(
            @Valid @RequestBody ZonaDomicilioDTO.ZonaDomicilioRequestDTO requestDTO) {
        ZonaDomicilioDTO.ZonaDomicilioResponseDTO response = zonaDomicilioService.crear(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ZonaDomicilioDTO.ZonaDomicilioResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ZonaDomicilioDTO.ZonaDomicilioRequestDTO requestDTO) {
        return ResponseEntity.ok(zonaDomicilioService.actualizar(id, requestDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        zonaDomicilioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
