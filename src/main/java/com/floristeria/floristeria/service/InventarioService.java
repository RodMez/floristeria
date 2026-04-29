package com.floristeria.floristeria.service;

import com.floristeria.floristeria.dto.InventarioResponseDTO;
import com.floristeria.floristeria.dto.InventarioUpdateRequestDTO;

public interface InventarioService {

    InventarioResponseDTO actualizarInventarioLocal(Integer inventarioId, InventarioUpdateRequestDTO request,
            Integer usuarioSedeId, String rol);
}
