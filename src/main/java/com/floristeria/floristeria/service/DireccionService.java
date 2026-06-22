package com.floristeria.floristeria.service;

import com.floristeria.floristeria.dto.DireccionRequestDTO;
import com.floristeria.floristeria.dto.DireccionResponseDTO;

import java.util.List;

public interface DireccionService {

    List<DireccionResponseDTO> listarMisDirecciones(Integer clienteId);

    DireccionResponseDTO crearDireccion(Integer clienteId, DireccionRequestDTO request);

    DireccionResponseDTO actualizarDireccion(Integer direccionId, DireccionRequestDTO request, Integer clienteId);

    void eliminarDireccion(Integer direccionId, Integer clienteId);
}