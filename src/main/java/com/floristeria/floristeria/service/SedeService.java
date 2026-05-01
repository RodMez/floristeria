package com.floristeria.floristeria.service;

import com.floristeria.floristeria.dto.SedeRequestDTO;
import com.floristeria.floristeria.dto.SedeResponseDTO;

import java.util.List;

public interface SedeService {

    List<SedeResponseDTO> listarTodas();

    SedeResponseDTO crearSede(SedeRequestDTO requestDTO);

    SedeResponseDTO actualizarSede(Integer id, SedeRequestDTO requestDTO);

    void eliminarSede(Integer id);
}
