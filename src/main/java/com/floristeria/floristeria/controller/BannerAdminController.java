package com.floristeria.floristeria.controller;

import com.floristeria.floristeria.dto.BannerRequestDTO;
import com.floristeria.floristeria.dto.BannerResponseDTO;
import com.floristeria.floristeria.service.BannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
public class BannerAdminController {

    private final BannerService bannerService;

    @GetMapping
    public ResponseEntity<List<BannerResponseDTO>> listarTodos() {
        return ResponseEntity.ok(bannerService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BannerResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(bannerService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<BannerResponseDTO> crear(@Valid @RequestBody BannerRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bannerService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BannerResponseDTO> actualizar(@PathVariable Integer id, @Valid @RequestBody BannerRequestDTO request) {
        return ResponseEntity.ok(bannerService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        bannerService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
