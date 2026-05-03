package com.floristeria.floristeria.service;

import com.floristeria.floristeria.dto.PedidoAdminResponseDTO;
import com.floristeria.floristeria.dto.PedidoRequestDTO;

import java.util.List;

public interface PedidoService {

    Integer crearPedido(PedidoRequestDTO request);

    List<PedidoAdminResponseDTO> obtenerPedidosPorSede(Integer sedeId);
}
