package com.floristeria.floristeria.controller;

import com.floristeria.floristeria.dto.ProductoComplementarioRequestDTO;
import com.floristeria.floristeria.dto.ProductoComplementarioResponseDTO;
import com.floristeria.floristeria.dto.ProductoRequestDTO;
import com.floristeria.floristeria.dto.ProductoResponseDTO;
import com.floristeria.floristeria.service.ProductoComplementarioService;
import com.floristeria.floristeria.service.ProductoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/superadmin/productos")
@RequiredArgsConstructor
public class ProductoAdminController {

    private final ProductoService productoService;
    private final ProductoComplementarioService complementoService;

    @GetMapping
    public ResponseEntity<List<ProductoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(productoService.listarTodos());
    }

    @PostMapping
    public ResponseEntity<ProductoResponseDTO> crearProducto(
            @Valid @RequestBody ProductoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crearProducto(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> actualizarProducto(
            @PathVariable Integer id,
            @Valid @RequestBody ProductoRequestDTO request) {
        return ResponseEntity.ok(productoService.actualizarProducto(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Integer id) {
        productoService.eliminarProducto(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{productoId}/complementos")
    public ResponseEntity<List<ProductoComplementarioResponseDTO>> listarComplementos(
            @PathVariable Integer productoId) {
        return ResponseEntity.ok(complementoService.listarPorProducto(productoId));
    }

    @PostMapping("/{productoId}/complementos")
    public ResponseEntity<ProductoComplementarioResponseDTO> crearComplemento(
            @PathVariable Integer productoId,
            @Valid @RequestBody ProductoComplementarioRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(complementoService.crear(productoId, request));
    }

    @PutMapping("/{productoId}/complementos/{id}")
    public ResponseEntity<ProductoComplementarioResponseDTO> actualizarComplemento(
            @PathVariable Integer productoId,
            @PathVariable Integer id,
            @Valid @RequestBody ProductoComplementarioRequestDTO request) {
        return ResponseEntity.ok(complementoService.actualizar(id, request));
    }

    @DeleteMapping("/{productoId}/complementos/{id}")
    public ResponseEntity<Void> eliminarComplemento(
            @PathVariable Integer productoId,
            @PathVariable Integer id) {
        complementoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
