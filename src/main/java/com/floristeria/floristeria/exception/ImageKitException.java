package com.floristeria.floristeria.exception;

public class ImageKitException extends RuntimeException {

    private final int statusCode;

    public ImageKitException(String mensaje, int statusCode) {
        super(mensaje);
        this.statusCode = statusCode;
    }

    public ImageKitException(String mensaje, Throwable cause, int statusCode) {
        super(mensaje, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMensajeUsuario() {
        if (statusCode == 429) {
            return "ImageKit esta en limite del plan. Reintenta en unos minutos.";
        }
        if (statusCode >= 400 && statusCode < 500) {
            return "Archivo rechazado por ImageKit: " + getMessage();
        }
        if (statusCode >= 500) {
            return "ImageKit no disponible (timeout o error de servidor). Reintenta.";
        }
        return "Error desconocido en ImageKit: " + getMessage();
    }
}
