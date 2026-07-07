package com.floristeria.floristeria.service.impl;

import com.floristeria.floristeria.dto.UsuarioAdminRequestDTO;
import com.floristeria.floristeria.dto.UsuarioAdminResponseDTO;
import com.floristeria.floristeria.entity.Sede;
import com.floristeria.floristeria.entity.UsuarioAdmin;
import com.floristeria.floristeria.repository.SedeRepository;
import com.floristeria.floristeria.repository.UsuarioAdminRepository;
import com.floristeria.floristeria.service.UsuarioAdminService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioAdminServiceImpl implements UsuarioAdminService {

    private final UsuarioAdminRepository usuarioAdminRepository;
    private final SedeRepository sedeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioAdminResponseDTO> listarTodos() {
        return usuarioAdminRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UsuarioAdminResponseDTO crearUsuario(UsuarioAdminRequestDTO request) {
        String email = request.getEmail().toLowerCase().trim();

        if (usuarioAdminRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        Sede sede = null;
        if (request.getSedeId() != null) {
            sede = sedeRepository.findById(request.getSedeId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Sede no encontrada con id: " + request.getSedeId()));
        }

        UsuarioAdmin usuario = new UsuarioAdmin();
        usuario.setEmail(email);
        usuario.setNombre(request.getNombre());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setRol(request.getRol());
        usuario.setSede(sede);

        UsuarioAdmin guardado = usuarioAdminRepository.save(usuario);
        return toResponseDTO(guardado);
    }

    @Override
    public UsuarioAdminResponseDTO actualizarUsuario(Integer id, UsuarioAdminRequestDTO request) {
        UsuarioAdmin usuario = usuarioAdminRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con id: " + id));

        Sede sede = null;
        if (request.getSedeId() != null) {
            sede = sedeRepository.findById(request.getSedeId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Sede no encontrada con id: " + request.getSedeId()));
        }

        usuario.setEmail(request.getEmail().toLowerCase().trim());
        usuario.setNombre(request.getNombre());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        usuario.setRol(request.getRol());
        usuario.setSede(sede);

        UsuarioAdmin guardado = usuarioAdminRepository.save(usuario);
        return toResponseDTO(guardado);
    }

    @Override
    public void eliminarUsuario(Integer id) {
        UsuarioAdmin usuario = usuarioAdminRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con id: " + id));

        usuarioAdminRepository.delete(usuario);
    }

    private UsuarioAdminResponseDTO toResponseDTO(UsuarioAdmin usuario) {
        return UsuarioAdminResponseDTO.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .rol(usuario.getRol())
                .sedeId(usuario.getSede() != null ? usuario.getSede().getId() : null)
                .sedeNombre(usuario.getSede() != null ? usuario.getSede().getNombre() : null)
                .build();
    }
}
