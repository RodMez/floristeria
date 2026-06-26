package com.floristeria.floristeria.controller;

import com.floristeria.floristeria.service.PedidoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final PedidoService pedidoService;

    @PostMapping("/wompi")
    public ResponseEntity<Void> recibirEventoWompi(@RequestBody Map<String, Object> payload) {
        try {
            pedidoService.procesarWebhookWompi(payload);
        } catch (IllegalArgumentException | SecurityException | IllegalStateException e) {
            log.error("Webhook Wompi - error de negocio (no reintentable): {}", e.getMessage(), e);
        }
        return ResponseEntity.ok().build();
    }
}
