package com.floristeria.floristeria.controller;

import com.floristeria.floristeria.dto.BannerResponseDTO;
import com.floristeria.floristeria.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
public class BannerPublicController {

    private final BannerService bannerService;

    @GetMapping
    public ResponseEntity<List<BannerResponseDTO>> listarPorUbicacion(
            @RequestParam String ubicacion,
            @RequestParam(required = false) Integer sedeId) {
        return ResponseEntity.ok(bannerService.listarPorUbicacion(ubicacion, sedeId));
    }
}
