package com.floristeria.floristeria.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.floristeria.floristeria.service.ImageKitService;
import com.floristeria.floristeria.util.MimeValidator;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/superadmin/imagenes")
@RequiredArgsConstructor
public class ImageKitController {

    private final ImageKitService imagekitService;
    private final MimeValidator mimeValidator;

    @PostMapping
    public ResponseEntity<Map<String, String>> subirImagen(
            @RequestParam("archivo") MultipartFile archivo) {

        MimeValidator.ResultadoMime resultado = mimeValidator.validar(archivo);
        String nombreArchivo = UUID.randomUUID() + resultado.extension();
        String url = imagekitService.subirImagen(archivo, nombreArchivo);

        Map<String, String> response = new HashMap<>();
        response.put("url", url);

        return ResponseEntity.ok(response);
    }
}
