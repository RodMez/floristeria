package com.floristeria.floristeria.service;

import org.springframework.web.multipart.MultipartFile;

import com.floristeria.floristeria.exception.ImageKitException;

public interface ImageKitService {

    String subirImagen(MultipartFile archivo, String nombreArchivo) throws ImageKitException;

    void borrar(String url) throws ImageKitException;
}
