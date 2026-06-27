package com.floristeria.floristeria.service.impl;

import com.floristeria.floristeria.dto.DireccionRequestDTO;
import com.floristeria.floristeria.dto.DireccionResponseDTO;
import com.floristeria.floristeria.entity.Cliente;
import com.floristeria.floristeria.entity.Direccion;
import com.floristeria.floristeria.entity.ZonaDomicilio;
import com.floristeria.floristeria.repository.ClienteRepository;
import com.floristeria.floristeria.repository.DireccionRepository;
import com.floristeria.floristeria.repository.ZonaDomicilioRepository;
import com.floristeria.floristeria.service.DireccionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;

@Service
@RequiredArgsConstructor
public class DireccionServiceImpl implements DireccionService {

    private final DireccionRepository direccionRepository;
    private final ClienteRepository clienteRepository;
    private final ZonaDomicilioRepository zonaDomicilioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DireccionResponseDTO> listarMisDirecciones(Integer clienteId) {
        // Validar que el cliente existe
        clienteRepository.findById(clienteId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));

        return direccionRepository.findByClienteId(clienteId).stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public DireccionResponseDTO crearDireccion(Integer clienteId, DireccionRequestDTO request) {
        // Validar que el cliente existe
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));

        ZonaDomicilio zonaDomicilio = zonaDomicilioRepository.findById(request.getZonaDomicilioId())
                .orElseThrow(() -> new EntityNotFoundException("Zona de domicilio no encontrada"));

        Direccion direccion = Direccion.builder()
                .cliente(cliente)
                .alias(request.getAlias())
                .direccion(request.getDireccion())
                .ciudad(request.getCiudad())
                .detalles(request.getDetalles())
                .zonaDomicilio(zonaDomicilio)
                .build();

        Direccion savedDireccion = direccionRepository.save(direccion);
        return mapToResponseDTO(savedDireccion);
    }

    @Override
    @Transactional
    public DireccionResponseDTO actualizarDireccion(Integer direccionId, DireccionRequestDTO request, Integer clienteId) {
        Direccion direccion = direccionRepository.findById(direccionId)
                .orElseThrow(() -> new EntityNotFoundException("Dirección no encontrada"));

        if (!direccion.getCliente().getId().equals(clienteId)) {
            throw new AccessDeniedException("No tienes permiso para editar esta dirección");
        }

        ZonaDomicilio zonaDomicilio = zonaDomicilioRepository.findById(request.getZonaDomicilioId())
                .orElseThrow(() -> new EntityNotFoundException("Zona de domicilio no encontrada"));

        direccion.setAlias(request.getAlias());
        direccion.setDireccion(request.getDireccion());
        direccion.setDetalles(request.getDetalles());
        direccion.setZonaDomicilio(zonaDomicilio);

        direccionRepository.save(direccion);
        return mapToResponseDTO(direccion);
    }

    @Override
    @Transactional
    public void eliminarDireccion(Integer direccionId, Integer clienteId) {
        Direccion direccion = direccionRepository.findById(direccionId)
                .orElseThrow(() -> new EntityNotFoundException("Dirección no encontrada"));

        if (!direccion.getCliente().getId().equals(clienteId)) {
            throw new AccessDeniedException("No tienes permiso para eliminar esta dirección");
        }

        direccion.setDeletedAt(LocalDateTime.now());
        direccionRepository.save(direccion);
    }

    private DireccionResponseDTO mapToResponseDTO(Direccion direccion) {
        return DireccionResponseDTO.builder()
                .id(direccion.getId())
                .alias(direccion.getAlias())
                .direccion(direccion.getDireccion())
                .ciudad(direccion.getCiudad())
                .detalles(direccion.getDetalles())
                .zonaDomicilioId(direccion.getZonaDomicilio() != null ? direccion.getZonaDomicilio().getId() : null)
                .zonaDomicilioNombre(
                    direccion.getZonaDomicilio() != null
                        ? direccion.getZonaDomicilio().getLocalidad()
                          + (direccion.getZonaDomicilio().getBarrio() != null
                              ? " - " + direccion.getZonaDomicilio().getBarrio()
                              : "")
                        : null
                )
                .build();
    }
}