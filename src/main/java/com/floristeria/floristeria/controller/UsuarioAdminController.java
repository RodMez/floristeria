package com.floristeria.floristeria.controller;

import com.floristeria.floristeria.dto.UsuarioAdminRequestDTO;
import com.floristeria.floristeria.dto.UsuarioAdminResponseDTO;
import com.floristeria.floristeria.service.UsuarioAdminService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/superadmin/usuarios")
@RequiredArgsConstructor
public class UsuarioAdminController {

    private final UsuarioAdminService usuarioAdminService;

    @GetMapping
    public ResponseEntity<List<UsuarioAdminResponseDTO>> listarTodos() {
        return ResponseEntity.ok(usuarioAdminService.listarTodos());
    }

    @PostMapping
    public ResponseEntity<UsuarioAdminResponseDTO> crearUsuario(
            @Validated(UsuarioAdminRequestDTO.Create.class) @RequestBody UsuarioAdminRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioAdminService.crearUsuario(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioAdminResponseDTO> actualizarUsuario(
            @PathVariable Integer id,
            @Validated(UsuarioAdminRequestDTO.Update.class) @RequestBody UsuarioAdminRequestDTO request) {
        return ResponseEntity.ok(usuarioAdminService.actualizarUsuario(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Integer id) {
        usuarioAdminService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }
}
