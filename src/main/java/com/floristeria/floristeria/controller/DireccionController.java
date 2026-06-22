package com.floristeria.floristeria.controller;

import com.floristeria.floristeria.dto.DireccionRequestDTO;
import com.floristeria.floristeria.dto.DireccionResponseDTO;
import com.floristeria.floristeria.security.ClienteDetails;
import com.floristeria.floristeria.service.DireccionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clientes/direcciones")
@RequiredArgsConstructor
public class DireccionController {

    private final DireccionService direccionService;

    @GetMapping
    public ResponseEntity<List<DireccionResponseDTO>> listarMisDirecciones(
            @AuthenticationPrincipal ClienteDetails clienteDetails) {
        Integer clienteId = clienteDetails.getClienteId();
        List<DireccionResponseDTO> direcciones = direccionService.listarMisDirecciones(clienteId);
        return ResponseEntity.ok(direcciones);
    }

    @PostMapping
    public ResponseEntity<DireccionResponseDTO> crearDireccion(
            @AuthenticationPrincipal ClienteDetails clienteDetails,
            @Valid @RequestBody DireccionRequestDTO request) {
        Integer clienteId = clienteDetails.getClienteId();
        DireccionResponseDTO response = direccionService.crearDireccion(clienteId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DireccionResponseDTO> actualizarDireccion(
            @PathVariable Integer id,
            @AuthenticationPrincipal ClienteDetails clienteDetails,
            @Valid @RequestBody DireccionRequestDTO request) {
        Integer clienteId = clienteDetails.getClienteId();
        DireccionResponseDTO response = direccionService.actualizarDireccion(id, request, clienteId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarDireccion(
            @PathVariable Integer id,
            @AuthenticationPrincipal ClienteDetails clienteDetails) {
        Integer clienteId = clienteDetails.getClienteId();
        direccionService.eliminarDireccion(id, clienteId);
        return ResponseEntity.noContent().build();
    }
}