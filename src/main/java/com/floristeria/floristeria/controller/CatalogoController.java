package com.floristeria.floristeria.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.floristeria.floristeria.dto.ProductoCatalogoDTO;
import com.floristeria.floristeria.dto.ProductoCatalogoDetalleDTO;
import com.floristeria.floristeria.service.CatalogoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/catalogo")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService catalogoService;

    @GetMapping("/sede/{sedeId}")
    public ResponseEntity<List<ProductoCatalogoDTO>> obtenerCatalogoPorSede(@PathVariable Integer sedeId) {
        List<ProductoCatalogoDTO> catalogo = catalogoService.obtenerCatalogoPorSede(sedeId);
        return ResponseEntity.ok(catalogo);
    }

    @GetMapping("/sede/{sedeId}/producto/{productoId}")
    public ResponseEntity<ProductoCatalogoDetalleDTO> obtenerDetalleProducto(
            @PathVariable Integer sedeId,
            @PathVariable Integer productoId) {
        ProductoCatalogoDetalleDTO detalle = catalogoService.obtenerDetalleProductoPorSede(sedeId, productoId);
        return ResponseEntity.ok(detalle);
    }

    @GetMapping(value = "/meta-feed", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> generarMetaFeed() {
        String xml = catalogoService.generarMetaFeedXml();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }
}
