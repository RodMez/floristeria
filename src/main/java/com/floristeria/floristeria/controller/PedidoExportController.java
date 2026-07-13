package com.floristeria.floristeria.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.floristeria.floristeria.entity.EstadoPedido;
import com.floristeria.floristeria.security.UsuarioDetails;
import com.floristeria.floristeria.service.PedidoExportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/pedidos")
@RequiredArgsConstructor
public class PedidoExportController {

    private final PedidoExportService pedidoExportService;

    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportarPedidosExcel(
            @AuthenticationPrincipal UsuarioDetails usuario,
            @RequestParam(required = false) Integer sedeId,
            @RequestParam(required = false) EstadoPedido estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) throws IOException {

        Integer sedeIdFiltro = sedeId;
        if (usuario.getSedeId() != null) {
            if (sedeIdFiltro != null && !sedeIdFiltro.equals(usuario.getSedeId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            sedeIdFiltro = usuario.getSedeId();
        }

        byte[] excelBytes = pedidoExportService.exportarPedidosExcel(sedeIdFiltro, estado, fechaInicio, fechaFin);

        String nombreArchivo = "pedidos_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(excelBytes.length)
                .body(excelBytes);
    }
}
