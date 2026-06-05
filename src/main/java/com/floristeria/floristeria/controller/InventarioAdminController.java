package com.floristeria.floristeria.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.floristeria.floristeria.dto.InventarioResponseDTO;
import com.floristeria.floristeria.dto.InventarioUpdateRequestDTO;
import com.floristeria.floristeria.security.UsuarioDetails;
import com.floristeria.floristeria.service.InventarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/admin/inventario")
@RequiredArgsConstructor
public class InventarioAdminController {

    private final InventarioService inventarioService;

    @GetMapping
    public ResponseEntity<List<InventarioResponseDTO>> obtenerInventario(
            @AuthenticationPrincipal UsuarioDetails usuario) {
        List<InventarioResponseDTO> inventario = inventarioService.obtenerInventarioPorSede(usuario.getSedeId());
        return ResponseEntity.ok(inventario);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventarioResponseDTO> actualizarInventario(
            @PathVariable Integer id,
            @Valid @RequestBody InventarioUpdateRequestDTO request,
            @AuthenticationPrincipal UsuarioDetails usuario) {

        InventarioResponseDTO response = inventarioService.actualizarInventarioLocal(
                id, request, usuario.getSedeId(), usuario.getRol());
        return ResponseEntity.ok(response);
    }
}
