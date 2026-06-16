package com.floristeria.floristeria.controller;

import com.floristeria.floristeria.dto.PedidoClienteRequestDTO;
import com.floristeria.floristeria.dto.PedidoClienteResponseDTO;
import com.floristeria.floristeria.security.ClienteDetails;
import com.floristeria.floristeria.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clientes/pedidos")
@RequiredArgsConstructor
public class PedidoClienteController {

    private final PedidoService pedidoService;

    @PostMapping
    public ResponseEntity<PedidoClienteResponseDTO> crearPedido(
            @AuthenticationPrincipal ClienteDetails clienteDetails,
            @Valid @RequestBody PedidoClienteRequestDTO request) {
        Integer clienteId = clienteDetails.getClienteId();
        PedidoClienteResponseDTO response = pedidoService.crearPedidoCliente(request, clienteId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}