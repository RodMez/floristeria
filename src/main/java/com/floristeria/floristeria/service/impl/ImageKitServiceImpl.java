package com.floristeria.floristeria.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.floristeria.floristeria.exception.ImageKitException;
import com.floristeria.floristeria.service.ImageKitService;

import io.imagekit.sdk.ImageKit;
import io.imagekit.sdk.models.FileCreateRequest;
import io.imagekit.sdk.models.results.Result;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImageKitServiceImpl implements ImageKitService {

    private static final Logger log = LoggerFactory.getLogger(ImageKitServiceImpl.class);

    private final ImageKit imageKit;

    @Override
    public String subirImagen(MultipartFile archivo, String nombreArchivo) {
        try {
            FileCreateRequest fileCreateRequest = new FileCreateRequest(archivo.getBytes(), nombreArchivo);
            fileCreateRequest.setFolder("/floristeria/productos");

            Result result = imageKit.upload(fileCreateRequest);

            return result.getUrl();
        } catch (ImageKitException e) {
            throw e;
        } catch (Throwable t) {
            throw mapearError(t, "subir imagen");
        }
    }

    @Override
    public void borrar(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        try {
            String filePath = extraerPathDeUrl(url);
            imageKit.deleteFile(filePath);
            log.info("Asset borrado de ImageKit: path={}", filePath);
        } catch (Throwable t) {
            String msg = t.getMessage() != null ? t.getMessage().toLowerCase() : "";
            if (msg.contains("not found") || msg.contains("404")) {
                log.warn("Asset no encontrado en ImageKit (ya borrado?): url={}, causa={}", url, t.getMessage());
            } else {
                log.warn("No se pudo borrar asset en ImageKit: url={}, causa={}", url, t.getMessage());
            }
        }
    }

    private String extraerPathDeUrl(String url) {
        String endpoint = imageKit.getConfig().getUrlEndpoint();
        if (endpoint != null && url.startsWith(endpoint)) {
            String path = url.substring(endpoint.length());
            int queryIdx = path.indexOf('?');
            if (queryIdx > 0) {
                path = path.substring(0, queryIdx);
            }
            return path;
        }
        int queryIdx = url.indexOf('?');
        if (queryIdx > 0) {
            return url.substring(0, queryIdx);
        }
        return url;
    }

    private ImageKitException mapearError(Throwable t, String operacion) {
        int status = extraerStatusCode(t);
        String msg = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();

        if (status == 429) {
            return new ImageKitException(operacion + ": rate-limit de ImageKit - " + msg, t, 429);
        }
        if (status >= 400 && status < 500) {
            return new ImageKitException(operacion + ": rechazado por ImageKit - " + msg, t, 400);
        }
        if (status >= 500 || esTimeout(t)) {
            return new ImageKitException(operacion + ": ImageKit no disponible - " + msg, t, 503);
        }
        return new ImageKitException(operacion + ": error desconocido - " + msg, t, 502);
    }

    private int extraerStatusCode(Throwable t) {
        try {
            var method = t.getClass().getMethod("getStatusCode");
            Object val = method.invoke(t);
            if (val instanceof Integer) {
                return (Integer) val;
            }
        } catch (Exception ignored) {}
        String msg = t.getMessage();
        if (msg != null) {
            if (msg.contains("429") || msg.toLowerCase().contains("rate limit")) return 429;
            if (msg.contains("400")) return 400;
            if (msg.contains("404")) return 404;
            if (msg.contains("500")) return 500;
        }
        return 0;
    }

    private boolean esTimeout(Throwable t) {
        if (t instanceof java.net.SocketTimeoutException
                || t instanceof java.util.concurrent.TimeoutException) {
            return true;
        }
        if (t.getCause() != null) {
            return esTimeout(t.getCause());
        }
        return false;
    }
}
