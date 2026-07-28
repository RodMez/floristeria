package com.floristeria.floristeria.service;

import com.floristeria.floristeria.entity.ConfiguracionTienda;

public interface ConfiguracionTiendaService {

    ConfiguracionTienda obtenerConfiguracion();

    ConfiguracionTienda actualizarConfiguracion(String correo, Boolean enviar, String whatsapp,
            String instagram, String facebook, String tiktok, String heroUrl, String bannerUrl,
            String nombreSitio, String tagline, String descripcion, String logoUrl, String iconUrl,
            String historia, String mision, String vision,
            String showcaseBadge, String showcaseTitulo, String showcaseSubtitulo,
            String nit, String razonSocial, String representanteLegal, String domicilioComercial,
            String correoHabeasData);
}
