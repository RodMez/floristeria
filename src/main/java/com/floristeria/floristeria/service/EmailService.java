package com.floristeria.floristeria.service;

import com.floristeria.floristeria.entity.Pedido;

public interface EmailService {

    void notificarNuevaVenta(Pedido pedido);
}
