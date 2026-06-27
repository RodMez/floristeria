package com.floristeria.floristeria.controller;

import com.floristeria.floristeria.dto.ZonaDomicilioDTO;
import com.floristeria.floristeria.security.UsuarioDetails;
import com.floristeria.floristeria.service.ZonaDomicilioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/zonas-domicilio")
@RequiredArgsConstructor
public class ZonaDomicilioAdminController {

    private final ZonaDomicilioService zonaDomicilioService;

    @GetMapping
    public ResponseEntity<List<ZonaDomicilioDTO.ZonaDomicilioResponseDTO>> listarPorSede(
            @AuthenticationPrincipal UsuarioDetails usuario) {
        return ResponseEntity.ok(zonaDomicilioService.listarPorSede(usuario.getSedeId()));
    }

    @PostMapping
    public ResponseEntity<ZonaDomicilioDTO.ZonaDomicilioResponseDTO> crear(
            @AuthenticationPrincipal UsuarioDetails usuario,
            @Valid @RequestBody ZonaDomicilioDTO.ZonaDomicilioRequestDTO requestDTO) {
        ZonaDomicilioDTO.ZonaDomicilioResponseDTO response = zonaDomicilioService.crearConSede(
                usuario.getSedeId(), requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ZonaDomicilioDTO.ZonaDomicilioResponseDTO> actualizar(
            @PathVariable Integer id,
            @AuthenticationPrincipal UsuarioDetails usuario,
            @Valid @RequestBody ZonaDomicilioDTO.ZonaDomicilioRequestDTO requestDTO) {
        return ResponseEntity.ok(zonaDomicilioService.actualizarConSede(
                id, usuario.getSedeId(), requestDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Integer id,
            @AuthenticationPrincipal UsuarioDetails usuario) {
        zonaDomicilioService.eliminarVerificandoSede(id, usuario.getSedeId());
        return ResponseEntity.noContent().build();
    }
}
