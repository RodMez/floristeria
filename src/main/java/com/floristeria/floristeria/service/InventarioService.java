package com.floristeria.floristeria.service;

import com.floristeria.floristeria.dto.InventarioResponseDTO;
import com.floristeria.floristeria.dto.InventarioUpdateRequestDTO;

import java.util.List;

public interface InventarioService {

    InventarioResponseDTO actualizarInventarioLocal(Integer inventarioId, InventarioUpdateRequestDTO request,
            Integer usuarioSedeId, String rol);

    List<InventarioResponseDTO> obtenerInventarioPorSede(Integer sedeId);
}
