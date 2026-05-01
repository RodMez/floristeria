package com.floristeria.floristeria.controller;

import com.floristeria.floristeria.dto.SedeRequestDTO;
import com.floristeria.floristeria.dto.SedeResponseDTO;
import com.floristeria.floristeria.service.SedeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/superadmin/sedes")
@RequiredArgsConstructor
public class SedeAdminController {

    private final SedeService sedeService;

    @GetMapping
    public ResponseEntity<List<SedeResponseDTO>> listarTodas() {
        return ResponseEntity.ok(sedeService.listarTodas());
    }

    @PostMapping
    public ResponseEntity<SedeResponseDTO> crearSede(@Valid @RequestBody SedeRequestDTO requestDTO) {
        SedeResponseDTO response = sedeService.crearSede(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SedeResponseDTO> actualizarSede(
            @PathVariable Integer id,
            @Valid @RequestBody SedeRequestDTO requestDTO) {
        SedeResponseDTO response = sedeService.actualizarSede(id, requestDTO);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarSede(@PathVariable Integer id) {
        sedeService.eliminarSede(id);
        return ResponseEntity.noContent().build();
    }
}
