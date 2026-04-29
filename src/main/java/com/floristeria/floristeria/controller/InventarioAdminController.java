package com.floristeria.floristeria.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.floristeria.floristeria.dto.InventarioResponseDTO;
import com.floristeria.floristeria.dto.InventarioUpdateRequestDTO;
import com.floristeria.floristeria.security.UsuarioDetails;
import com.floristeria.floristeria.service.InventarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/inventario")
@RequiredArgsConstructor
public class InventarioAdminController {

    private final InventarioService inventarioService;

    @PutMapping("/{id}")
    public ResponseEntity<InventarioResponseDTO> actualizarInventario(
            @PathVariable Integer id,
            @Valid @RequestBody InventarioUpdateRequestDTO request,
            Authentication authentication) {

        UsuarioDetails usuarioDetails = (UsuarioDetails) authentication.getPrincipal();
        String rol = usuarioDetails.getRol();
        Integer sedeId = usuarioDetails.getSedeId();

        InventarioResponseDTO response = inventarioService.actualizarInventarioLocal(id, request, sedeId, rol);
        return ResponseEntity.ok(response);
    }
}
