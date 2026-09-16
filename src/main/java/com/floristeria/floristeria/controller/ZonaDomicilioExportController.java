package com.floristeria.floristeria.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.floristeria.floristeria.repository.SedeRepository;
import com.floristeria.floristeria.security.UsuarioDetails;
import com.floristeria.floristeria.service.ZonaDomicilioExportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/zonas-domicilio")
@RequiredArgsConstructor
public class ZonaDomicilioExportController {

    private final ZonaDomicilioExportService exportService;
    private final SedeRepository sedeRepository;

    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportarZonasExcel(
            @AuthenticationPrincipal UsuarioDetails usuario,
            @RequestParam(required = false) Integer sedeId) throws IOException {

        Integer sedeIdFiltro = sedeId;
        if (usuario.getSedeId() != null) {
            if (sedeIdFiltro != null && !sedeIdFiltro.equals(usuario.getSedeId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            sedeIdFiltro = usuario.getSedeId();
        }

        if (sedeIdFiltro != null && !sedeRepository.existsById(sedeIdFiltro)) {
            return ResponseEntity.notFound().build();
        }

        byte[] excelBytes = exportService.exportarZonasExcel(sedeIdFiltro);

        String suffix = sedeIdFiltro != null ? "sede" + sedeIdFiltro + "_" : "";
        String nombreArchivo = "zonas_domicilio_" + suffix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(excelBytes.length)
                .body(excelBytes);
    }
}
