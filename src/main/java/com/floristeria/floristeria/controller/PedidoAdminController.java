package com.floristeria.floristeria.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.floristeria.floristeria.dto.PedidoAdminResponseDTO;
import com.floristeria.floristeria.security.UsuarioDetails;
import com.floristeria.floristeria.service.PedidoService;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/pedidos")
@RequiredArgsConstructor
public class PedidoAdminController {

    private final PedidoService pedidoService;

    @GetMapping
    public ResponseEntity<List<PedidoAdminResponseDTO>> obtenerPedidos(Authentication authentication) {
        UsuarioDetails usuario = (UsuarioDetails) authentication.getPrincipal();
        List<PedidoAdminResponseDTO> pedidos = pedidoService.obtenerPedidosPorSede(usuario.getSedeId());
        return ResponseEntity.ok(pedidos);
    }
}
