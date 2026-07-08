package com.floristeria.floristeria.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.floristeria.floristeria.dto.ReseñaEstadoDTO;
import com.floristeria.floristeria.dto.ReseñaRequestDTO;
import com.floristeria.floristeria.dto.ReseñaResponseDTO;
import com.floristeria.floristeria.dto.ReseñasProductoResponseDTO;
import com.floristeria.floristeria.security.ClienteDetails;
import com.floristeria.floristeria.service.ReseñaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/resenas")
@RequiredArgsConstructor
public class ReseñaController {

    private final ReseñaService reseñaService;

    @GetMapping("/producto/{productoId}")
    public ResponseEntity<ReseñasProductoResponseDTO> obtenerPorProducto(
            @PathVariable Integer productoId) {
        return ResponseEntity.ok(reseñaService.obtenerPorProducto(productoId));
    }

    @GetMapping("/producto/{productoId}/estado")
    public ResponseEntity<ReseñaEstadoDTO> obtenerEstado(
            @AuthenticationPrincipal ClienteDetails clienteDetails,
            @PathVariable Integer productoId) {
        Integer clienteId = clienteDetails.getClienteId();
        return ResponseEntity.ok(reseñaService.obtenerEstadoCliente(clienteId, productoId));
    }

    @PostMapping
    public ResponseEntity<ReseñaResponseDTO> crear(
            @AuthenticationPrincipal ClienteDetails clienteDetails,
            @Valid @RequestBody ReseñaRequestDTO request) {
        Integer clienteId = clienteDetails.getClienteId();
        ReseñaResponseDTO response = reseñaService.crear(clienteId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
