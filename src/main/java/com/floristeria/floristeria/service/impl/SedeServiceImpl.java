package com.floristeria.floristeria.service.impl;

import com.floristeria.floristeria.dto.SedeRequestDTO;
import com.floristeria.floristeria.dto.SedeResponseDTO;
import com.floristeria.floristeria.entity.Inventario;
import com.floristeria.floristeria.entity.Producto;
import com.floristeria.floristeria.entity.Sede;
import com.floristeria.floristeria.entity.UsuarioAdmin;
import com.floristeria.floristeria.repository.InventarioRepository;
import com.floristeria.floristeria.repository.ProductoRepository;
import com.floristeria.floristeria.repository.SedeRepository;
import com.floristeria.floristeria.repository.UsuarioAdminRepository;
import com.floristeria.floristeria.service.SedeService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SedeServiceImpl implements SedeService {

    private final SedeRepository sedeRepository;
    private final ProductoRepository productoRepository;
    private final InventarioRepository inventarioRepository;
    private final UsuarioAdminRepository usuarioAdminRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SedeResponseDTO> listarTodas() {
        return sedeRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public SedeResponseDTO crearSede(SedeRequestDTO requestDTO) {
        // 1. Crear y guardar la nueva sede
        Sede sede = new Sede();
        sede.setNombre(requestDTO.getNombre());
        sede.setCiudad(requestDTO.getCiudad());
        sede.setWhatsapp(requestDTO.getTelefonoWhatsapp());
        sede.setInstagramUrl(requestDTO.getInstagramUrl());
        sede.setFacebookUrl(requestDTO.getFacebookUrl());

        Sede sedeGuardada = sedeRepository.save(sede);

        // 2. Sincronizar inventario: crear registros para todos los productos existentes
        List<Producto> todosProductos = productoRepository.findAll();

        List<Inventario> inventarios = todosProductos.stream()
                .map(producto -> Inventario.builder()
                        .sede(sedeGuardada)
                        .producto(producto)
                        .stock(0)
                        .precio(java.math.BigDecimal.ZERO)
                        .disponible(Boolean.FALSE)
                        .build())
                .collect(Collectors.toList());

        inventarioRepository.saveAll(inventarios);

        return toResponseDTO(sedeGuardada);
    }

    @Override
    public SedeResponseDTO actualizarSede(Integer id, SedeRequestDTO requestDTO) {
        Sede sede = sedeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sede no encontrada con id: " + id));

        sede.setNombre(requestDTO.getNombre());
        sede.setCiudad(requestDTO.getCiudad());
        sede.setWhatsapp(requestDTO.getTelefonoWhatsapp());
        sede.setInstagramUrl(requestDTO.getInstagramUrl());
        sede.setFacebookUrl(requestDTO.getFacebookUrl());

        Sede sedeActualizada = sedeRepository.save(sede);
        return toResponseDTO(sedeActualizada);
    }

    @Override
    public void eliminarSede(Integer id) {
        Sede sede = sedeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sede no encontrada con id: " + id));

        List<Inventario> inventarios = inventarioRepository.findBySede_Id(id);
        for (Inventario inv : inventarios) {
            inv.setDeletedAt(LocalDateTime.now());
            inventarioRepository.save(inv);
        }

        List<UsuarioAdmin> usuarios = usuarioAdminRepository.findBySede_Id(id);
        for (UsuarioAdmin user : usuarios) {
            user.setDeletedAt(LocalDateTime.now());
            usuarioAdminRepository.save(user);
        }

        sede.setDeletedAt(LocalDateTime.now());
        sedeRepository.save(sede);
    }

    private SedeResponseDTO toResponseDTO(Sede sede) {
        return SedeResponseDTO.builder()
                .id(sede.getId())
                .nombre(sede.getNombre())
                .ciudad(sede.getCiudad())
                .telefonoWhatsapp(sede.getWhatsapp())
                .instagramUrl(sede.getInstagramUrl())
                .facebookUrl(sede.getFacebookUrl())
                .build();
    }
}
