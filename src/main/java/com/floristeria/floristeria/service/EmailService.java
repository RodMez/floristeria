package com.floristeria.floristeria.service;

public interface EmailService {

    void notificarNuevaVenta(String codigoPedido);

    @Deprecated
    default void notificarNuevaVenta(com.floristeria.floristeria.entity.Pedido pedido) {
        if (pedido != null && pedido.getCodigo() != null) {
            notificarNuevaVenta(pedido.getCodigo());
        }
    }

    void enviarCorreoDirecto(String toEmail, String toName, String subject, String htmlContent);
}
