package com.floristeria.floristeria.controller;

import com.floristeria.floristeria.dto.ZonaDomicilioDTO;
import com.floristeria.floristeria.service.ZonaDomicilioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/zonas-domicilio")
@RequiredArgsConstructor
public class ZonaDomicilioPublicController {

    private final ZonaDomicilioService zonaDomicilioService;

    @GetMapping("/sede/{sedeId}")
    public ResponseEntity<List<ZonaDomicilioDTO.ZonaDomicilioResponseDTO>> listarPorSede(
            @PathVariable Integer sedeId) {
        return ResponseEntity.ok(zonaDomicilioService.listarPorSede(sedeId));
    }
}
