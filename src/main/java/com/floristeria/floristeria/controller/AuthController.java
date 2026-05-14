package com.floristeria.floristeria.controller;

import com.floristeria.floristeria.dto.AuthResponseDTO;
import com.floristeria.floristeria.dto.LoginRequestDTO;
import com.floristeria.floristeria.entity.UsuarioAdmin;
import com.floristeria.floristeria.repository.UsuarioAdminRepository;
import com.floristeria.floristeria.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioAdminRepository usuarioAdminRepository;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody LoginRequestDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UsuarioAdmin usuario = usuarioAdminRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String token = jwtService.generateToken(usuario);

        return ResponseEntity.ok(AuthResponseDTO.builder()
                .token(token)
                .rol(usuario.getRol())
                .sedeId(usuario.getSede() != null ? usuario.getSede().getId() : null)
                .build());
    }
}
