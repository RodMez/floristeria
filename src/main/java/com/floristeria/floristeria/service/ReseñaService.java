package com.floristeria.floristeria.service;

import java.util.List;

import com.floristeria.floristeria.dto.ReseñaEstadoDTO;
import com.floristeria.floristeria.dto.ReseñaRequestDTO;
import com.floristeria.floristeria.dto.ReseñaResponseDTO;
import com.floristeria.floristeria.dto.ReseñasProductoResponseDTO;

public interface ReseñaService {

    ReseñaResponseDTO crear(Integer clienteId, ReseñaRequestDTO request);

    ReseñasProductoResponseDTO obtenerPorProducto(Integer productoId);

    ReseñaEstadoDTO obtenerEstadoCliente(Integer clienteId, Integer productoId);

    List<ReseñaResponseDTO> listarTodas();

    List<ReseñaResponseDTO> listarPendientes();

    ReseñaResponseDTO aprobar(Integer id);

    void eliminar(Integer id);
}
