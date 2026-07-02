package com.floristeria.floristeria.service.impl;

import com.floristeria.floristeria.entity.ConfiguracionTienda;
import com.floristeria.floristeria.repository.ConfiguracionTiendaRepository;
import com.floristeria.floristeria.service.ConfiguracionTiendaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfiguracionTiendaServiceImpl implements ConfiguracionTiendaService {

    private final ConfiguracionTiendaRepository configuracionRepository;

    private static final Integer CONFIG_ID = 1;

    @Override
    @Transactional
    public ConfiguracionTienda obtenerConfiguracion() {
        return configuracionRepository.findById(CONFIG_ID)
                .orElseGet(() -> configuracionRepository.save(
                        ConfiguracionTienda.builder()
                                .correoMaestro(null)
                                .enviarCopiaMaestro(false)
                                .build()
                ));
    }

    @Override
    @Transactional
    public ConfiguracionTienda actualizarConfiguracion(String correo, Boolean enviar) {
        ConfiguracionTienda config = obtenerConfiguracion();
        config.setCorreoMaestro(correo);
        config.setEnviarCopiaMaestro(enviar);
        return configuracionRepository.save(config);
    }
}
