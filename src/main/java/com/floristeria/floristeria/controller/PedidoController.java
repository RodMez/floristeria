package com.floristeria.floristeria.controller;

import com.floristeria.floristeria.dto.PedidoRequestDTO;
import com.floristeria.floristeria.service.PedidoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    public ResponseEntity<Integer> crearPedido(@RequestBody PedidoRequestDTO request) {
        Integer pedidoId = pedidoService.crearPedido(request);
        return ResponseEntity.status(201).body(pedidoId);
    }
}