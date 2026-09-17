package com.floristeria.floristeria.exception;

public class FechaEntregaInvalidaException extends RuntimeException {

    private final String campo;

    public FechaEntregaInvalidaException(String message) {
        super(message);
        this.campo = "fechaEntrega";
    }

    public FechaEntregaInvalidaException(String campo, String message) {
        super(message);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
