package com.floristeria.floristeria.controller;

import com.floristeria.floristeria.dto.FechaBloqueadaRequestDTO;
import com.floristeria.floristeria.dto.FechaBloqueadaResponseDTO;
import com.floristeria.floristeria.security.UsuarioDetails;
import com.floristeria.floristeria.service.EntregaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/sedes/{sedeId}/fechas-bloqueadas")
@RequiredArgsConstructor
public class FechaBloqueadaAdminController {

    private final EntregaService entregaService;

    private void validarAcceso(Integer sedeId, UsuarioDetails usuario) {
        if (!"SUPERADMIN".equals(usuario.getRol()) && !sedeId.equals(usuario.getSedeId())) {
            throw new AccessDeniedException("No tiene permisos sobre esta sede");
        }
    }

    @GetMapping
    public ResponseEntity<List<FechaBloqueadaResponseDTO>> listar(
            @PathVariable Integer sedeId,
            @AuthenticationPrincipal UsuarioDetails usuario) {
        validarAcceso(sedeId, usuario);
        return ResponseEntity.ok(entregaService.listarBloqueos(sedeId));
    }

    @PostMapping
    public ResponseEntity<FechaBloqueadaResponseDTO> agregar(
            @PathVariable Integer sedeId,
            @Valid @RequestBody FechaBloqueadaRequestDTO request,
            @AuthenticationPrincipal UsuarioDetails usuario) {
        validarAcceso(sedeId, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(entregaService.agregarBloqueo(sedeId, request));
    }

    @DeleteMapping("/{bloqueoId}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Integer sedeId,
            @PathVariable Integer bloqueoId,
            @AuthenticationPrincipal UsuarioDetails usuario) {
        validarAcceso(sedeId, usuario);
        entregaService.eliminarBloqueo(sedeId, bloqueoId);
        return ResponseEntity.noContent().build();
    }
}
