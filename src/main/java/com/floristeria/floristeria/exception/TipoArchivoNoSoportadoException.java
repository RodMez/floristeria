package com.floristeria.floristeria.exception;

public class TipoArchivoNoSoportadoException extends RuntimeException {

    private final String mimeDetectado;

    public TipoArchivoNoSoportadoException(String mimeDetectado) {
        super("Tipo de archivo no soportado: " + mimeDetectado
                + ". Solo se permiten: image/jpeg, image/png, image/webp, image/avif");
        this.mimeDetectado = mimeDetectado;
    }

    public String getMimeDetectado() {
        return mimeDetectado;
    }
}
