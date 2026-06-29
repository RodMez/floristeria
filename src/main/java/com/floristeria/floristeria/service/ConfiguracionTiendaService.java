package com.floristeria.floristeria.service;

import com.floristeria.floristeria.entity.ConfiguracionTienda;

public interface ConfiguracionTiendaService {

    ConfiguracionTienda obtenerConfiguracion();

    ConfiguracionTienda actualizarConfiguracion(String correo, Boolean enviar);
}
