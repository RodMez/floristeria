package com.floristeria.floristeria.controller;

import com.floristeria.floristeria.dto.ClienteActualizarRequestDTO;
import com.floristeria.floristeria.dto.ClientePasswordRequestDTO;
import com.floristeria.floristeria.dto.ClientePerfilResponseDTO;
import com.floristeria.floristeria.security.ClienteDetails;
import com.floristeria.floristeria.service.ClienteAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clientes/perfil")
@RequiredArgsConstructor
public class ClientePerfilController {

    private final ClienteAuthService clienteAuthService;

    @GetMapping
    public ResponseEntity<ClientePerfilResponseDTO> obtenerPerfil(
            @AuthenticationPrincipal ClienteDetails clienteDetails) {
        ClientePerfilResponseDTO response = clienteAuthService.obtenerPerfil(clienteDetails.getClienteId());
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<ClientePerfilResponseDTO> actualizarPerfil(
            @AuthenticationPrincipal ClienteDetails clienteDetails,
            @Valid @RequestBody ClienteActualizarRequestDTO request) {
        Integer clienteId = clienteDetails.getClienteId();
        ClientePerfilResponseDTO response = clienteAuthService.actualizarPerfil(clienteId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> cambiarPassword(
            @AuthenticationPrincipal ClienteDetails clienteDetails,
            @Valid @RequestBody ClientePasswordRequestDTO request) {
        clienteAuthService.cambiarPassword(clienteDetails.getClienteId(), request);
        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente"));
    }
}
