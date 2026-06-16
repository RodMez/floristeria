package com.floristeria.floristeria.service.impl;

import com.floristeria.floristeria.dto.ClienteAuthResponseDTO;
import com.floristeria.floristeria.dto.ClienteLoginDTO;
import com.floristeria.floristeria.dto.ClienteRegistroDTO;
import com.floristeria.floristeria.entity.Cliente;
import com.floristeria.floristeria.repository.ClienteRepository;
import com.floristeria.floristeria.security.JwtService;
import com.floristeria.floristeria.service.ClienteAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClienteAuthServiceImpl implements ClienteAuthService {

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public ClienteAuthResponseDTO registrar(ClienteRegistroDTO request) {
        // Validar que el email no exista
        if (clienteRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        // Crear y guardar el cliente
        Cliente cliente = Cliente.builder()
                .nombre(request.getNombre())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .telefono(request.getTelefono())
                .build();

        cliente = clienteRepository.save(cliente);

        // Generar JWT
        String token = jwtService.generateToken(cliente);

        return ClienteAuthResponseDTO.builder()
                .token(token)
                .clienteId(cliente.getId())
                .nombre(cliente.getNombre())
                .email(cliente.getEmail())
                .rol("CLIENTE")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteAuthResponseDTO login(ClienteLoginDTO request) {
        // Buscar por email
        Cliente cliente = clienteRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

        // Verificar contraseña
        if (!passwordEncoder.matches(request.getPassword(), cliente.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }

        // Generar JWT
        String token = jwtService.generateToken(cliente);

        return ClienteAuthResponseDTO.builder()
                .token(token)
                .clienteId(cliente.getId())
                .nombre(cliente.getNombre())
                .email(cliente.getEmail())
                .rol("CLIENTE")
                .build();
    }
}