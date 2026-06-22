package com.floristeria.floristeria.service;

import com.floristeria.floristeria.dto.PedidoAdminResponseDTO;
import com.floristeria.floristeria.dto.PedidoClienteRequestDTO;
import com.floristeria.floristeria.dto.PedidoClienteResponseDTO;
import com.floristeria.floristeria.dto.PedidoHistorialDTO;
import com.floristeria.floristeria.dto.PedidoRequestDTO;
import com.floristeria.floristeria.entity.EstadoPedido;

import java.util.List;
import java.util.Map;

public interface PedidoService {

    Integer crearPedido(PedidoRequestDTO request);

    PedidoClienteResponseDTO crearPedidoCliente(PedidoClienteRequestDTO request, Integer clienteId);

    List<PedidoAdminResponseDTO> obtenerPedidosPorSede(Integer sedeId);

    PedidoAdminResponseDTO actualizarEstadoPedido(Integer pedidoId, EstadoPedido nuevoEstado, Integer usuarioSedeId, String rol);

    void procesarPagoExitoso(Integer pedidoId, String transaccionId, String metodoPago);

    void procesarWebhookWompi(Map<String, Object> payload);

    List<PedidoHistorialDTO> obtenerPedidosPorCliente(Integer clienteId);
}
