package com.floristeria.floristeria.controller;

import com.floristeria.floristeria.dto.ClienteAuthResponseDTO;
import com.floristeria.floristeria.dto.ClienteLoginDTO;
import com.floristeria.floristeria.dto.ClienteRegistroDTO;
import com.floristeria.floristeria.service.ClienteAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clientes/auth")
@RequiredArgsConstructor
public class ClienteAuthController {

    private final ClienteAuthService clienteAuthService;

    @PostMapping("/registro")
    public ResponseEntity<ClienteAuthResponseDTO> registrar(@Valid @RequestBody ClienteRegistroDTO request) {
        ClienteAuthResponseDTO response = clienteAuthService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ClienteAuthResponseDTO> login(@Valid @RequestBody ClienteLoginDTO request) {
        ClienteAuthResponseDTO response = clienteAuthService.login(request);
        return ResponseEntity.ok(response);
    }
}