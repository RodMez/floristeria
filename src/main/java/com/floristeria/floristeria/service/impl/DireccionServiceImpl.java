package com.floristeria.floristeria.service.impl;

import com.floristeria.floristeria.dto.DireccionRequestDTO;
import com.floristeria.floristeria.dto.DireccionResponseDTO;
import com.floristeria.floristeria.entity.Cliente;
import com.floristeria.floristeria.entity.Direccion;
import com.floristeria.floristeria.repository.ClienteRepository;
import com.floristeria.floristeria.repository.DireccionRepository;
import com.floristeria.floristeria.service.DireccionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DireccionServiceImpl implements DireccionService {

    private final DireccionRepository direccionRepository;
    private final ClienteRepository clienteRepository;

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

        Direccion direccion = Direccion.builder()
                .cliente(cliente)
                .alias(request.getAlias())
                .direccion(request.getDireccion())
                .ciudad(request.getCiudad())
                .detalles(request.getDetalles())
                .build();

        Direccion savedDireccion = direccionRepository.save(direccion);
        return mapToResponseDTO(savedDireccion);
    }

    private DireccionResponseDTO mapToResponseDTO(Direccion direccion) {
        return DireccionResponseDTO.builder()
                .id(direccion.getId())
                .alias(direccion.getAlias())
                .direccion(direccion.getDireccion())
                .ciudad(direccion.getCiudad())
                .detalles(direccion.getDetalles())
                .build();
    }
}