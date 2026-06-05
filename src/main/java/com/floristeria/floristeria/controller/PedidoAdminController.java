package com.floristeria.floristeria.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.floristeria.floristeria.dto.PedidoAdminResponseDTO;
import com.floristeria.floristeria.dto.PedidoEstadoUpdateRequestDTO;
import com.floristeria.floristeria.security.UsuarioDetails;
import com.floristeria.floristeria.service.PedidoService;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/pedidos")
@RequiredArgsConstructor
public class PedidoAdminController {

    private final PedidoService pedidoService;

    @GetMapping
    public ResponseEntity<List<PedidoAdminResponseDTO>> obtenerPedidos(
            @AuthenticationPrincipal UsuarioDetails usuario) {
        List<PedidoAdminResponseDTO> pedidos = pedidoService.obtenerPedidosPorSede(usuario.getSedeId());
        return ResponseEntity.ok(pedidos);
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<PedidoAdminResponseDTO> actualizarEstado(
            @PathVariable Integer id,
            @Valid @RequestBody PedidoEstadoUpdateRequestDTO request,
            @AuthenticationPrincipal UsuarioDetails usuario) {
        PedidoAdminResponseDTO updated = pedidoService.actualizarEstadoPedido(
                id, request.getEstado(), usuario.getSedeId(), usuario.getRol());
        return ResponseEntity.ok(updated);
    }
}
