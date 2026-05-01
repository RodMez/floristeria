package com.floristeria.floristeria.service;

import com.floristeria.floristeria.dto.CategoriaRequestDTO;
import com.floristeria.floristeria.dto.CategoriaResponseDTO;

import java.util.List;

public interface CategoriaService {

    List<CategoriaResponseDTO> listarTodas();

    CategoriaResponseDTO crearCategoria(CategoriaRequestDTO requestDTO);

    CategoriaResponseDTO actualizarCategoria(Integer id, CategoriaRequestDTO requestDTO);

    void eliminarCategoria(Integer id);
}
