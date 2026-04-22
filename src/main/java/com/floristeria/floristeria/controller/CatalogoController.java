package com.floristeria.floristeria.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.floristeria.floristeria.dto.ProductoCatalogoDTO;
import com.floristeria.floristeria.service.CatalogoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/catalogo")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CatalogoController {

    private final CatalogoService catalogoService;

    @GetMapping("/sede/{sedeId}")
    public ResponseEntity<List<ProductoCatalogoDTO>> obtenerCatalogoPorSede(@PathVariable Integer sedeId) {
        List<ProductoCatalogoDTO> catalogo = catalogoService.obtenerCatalogoPorSede(sedeId);
        return ResponseEntity.ok(catalogo);
    }
}