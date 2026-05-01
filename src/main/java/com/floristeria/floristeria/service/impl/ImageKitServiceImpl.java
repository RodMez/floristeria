package com.floristeria.floristeria.service.impl;

import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.floristeria.floristeria.service.ImageKitService;

import io.imagekit.sdk.ImageKit;
import io.imagekit.sdk.models.FileCreateRequest;
import io.imagekit.sdk.models.results.Result;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImageKitServiceImpl implements ImageKitService {

    private final ImageKit imageKit;

    @Override
    public String subirImagen(MultipartFile archivo, String nombreArchivo) throws IOException {
        try {
            FileCreateRequest fileCreateRequest = new FileCreateRequest(archivo.getBytes(), nombreArchivo);
            fileCreateRequest.setFolder("/floristeria/productos");

            Result result = imageKit.upload(fileCreateRequest);

            return result.getUrl();
        } catch (Exception e) {
            throw new IOException("Error al subir imagen a ImageKit: " + e.getMessage(), e);
        }
    }
}
