package com.floristeria.floristeria.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.floristeria.floristeria.security.UsuarioDetails;
import com.floristeria.floristeria.service.ProductoInventarioExportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/productos-inventario")
@RequiredArgsConstructor
public class ProductoInventarioExportController {

    private final ProductoInventarioExportService exportService;

    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportarProductosInventarioExcel(
            @AuthenticationPrincipal UsuarioDetails usuario) throws IOException {

        Integer sedeIdFiltro = usuario.getSedeId();

        byte[] excelBytes = exportService.exportarProductosInventarioExcel(sedeIdFiltro);

        String nombreArchivo = "productos_inventario_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(excelBytes.length)
                .body(excelBytes);
    }
}
