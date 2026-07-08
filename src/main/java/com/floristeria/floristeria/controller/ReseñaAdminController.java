package com.floristeria.floristeria.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.floristeria.floristeria.dto.ReseñaResponseDTO;
import com.floristeria.floristeria.service.ReseñaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/resenas")
@RequiredArgsConstructor
public class ReseñaAdminController {

    private final ReseñaService reseñaService;

    @GetMapping
    public ResponseEntity<List<ReseñaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(reseñaService.listarTodas());
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<ReseñaResponseDTO>> listarPendientes() {
        return ResponseEntity.ok(reseñaService.listarPendientes());
    }

    @PatchMapping("/{id}/aprobar")
    public ResponseEntity<ReseñaResponseDTO> aprobar(@PathVariable Integer id) {
        return ResponseEntity.ok(reseñaService.aprobar(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        reseñaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
