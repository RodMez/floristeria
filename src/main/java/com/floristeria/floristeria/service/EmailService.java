package com.floristeria.floristeria.service;

import com.floristeria.floristeria.entity.Pedido;

public interface EmailService {

    void notificarNuevaVenta(Pedido pedido);

    void enviarCorreoDirecto(String toEmail, String toName, String subject, String htmlContent);
}
