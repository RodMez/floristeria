package com.floristeria.floristeria.controller;

import com.floristeria.floristeria.dto.SedeResponseDTO;
import com.floristeria.floristeria.service.SedeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sedes")
@RequiredArgsConstructor
public class SedePublicController {

    private final SedeService sedeService;

    @GetMapping
    public ResponseEntity<List<SedeResponseDTO>> listarTodas() {
        return ResponseEntity.ok(sedeService.listarTodas());
    }
}
