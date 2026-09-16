package com.floristeria.floristeria.controller;

import com.floristeria.floristeria.dto.PedidoEstadoUpdateRequestDTO;
import com.floristeria.floristeria.dto.PedidoAdminResponseDTO;
import com.floristeria.floristeria.repository.PedidoRepository;
import com.floristeria.floristeria.security.UsuarioDetails;
import com.floristeria.floristeria.service.EmailService;
import com.floristeria.floristeria.service.PedidoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/pedidos")
@RequiredArgsConstructor
public class PedidoAdminController {

    private final PedidoService pedidoService;
    private final PedidoRepository pedidoRepository;
    private final EmailService emailService;

    @GetMapping
    public ResponseEntity<List<PedidoAdminResponseDTO>> obtenerPedidos(
            @AuthenticationPrincipal UsuarioDetails usuario) {
        List<PedidoAdminResponseDTO> pedidos = pedidoService.obtenerPedidosPorSede(usuario.getSedeId());
        return ResponseEntity.ok(pedidos);
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<PedidoAdminResponseDTO> actualizarEstado(
            @PathVariable String id,
            @Valid @RequestBody PedidoEstadoUpdateRequestDTO request,
            @AuthenticationPrincipal UsuarioDetails usuario) {
        PedidoAdminResponseDTO updated = pedidoService.actualizarEstadoPedido(
                id, request.getEstado(), usuario.getSedeId(), usuario.getRol());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{codigo}/reenviar-correo")
    public ResponseEntity<Map<String, Object>> reenviarCorreo(
            @PathVariable String codigo,
            @AuthenticationPrincipal UsuarioDetails usuario) {
        var pedido = pedidoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado: " + codigo));

        if (!"SUPERADMIN".equals(usuario.getRol()) && !pedido.getSede().getId().equals(usuario.getSedeId())) {
            return ResponseEntity.status(403).body(Map.of("mensaje", "No tiene permisos sobre este pedido"));
        }

        if (!"PAGADO".equals(pedido.getEstado().name())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "mensaje", "Solo se puede reenviar correo de pedidos en estado PAGADO (actual: " + pedido.getEstado().name() + ")",
                    "codigo", codigo,
                    "estado", pedido.getEstado().name()));
        }

        log.info("Reenvío manual de correo solicitado por {} para pedido {}", usuario.getUsername(), codigo);
        emailService.notificarNuevaVenta(codigo);
        return ResponseEntity.ok(Map.of(
                "mensaje", "Reenvío de correos disparado para pedido " + codigo,
                "codigo", codigo));
    }
}
