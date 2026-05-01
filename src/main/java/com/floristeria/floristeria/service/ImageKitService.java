package com.floristeria.floristeria.service;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

public interface ImageKitService {

    String subirImagen(MultipartFile archivo, String nombreArchivo) throws IOException;
}
