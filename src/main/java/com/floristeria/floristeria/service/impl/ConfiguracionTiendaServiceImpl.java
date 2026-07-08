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
    public ConfiguracionTienda actualizarConfiguracion(String correo, Boolean enviar, String whatsapp,
            String instagram, String facebook, String tiktok, String heroUrl, String bannerUrl,
            String nombreSitio, String tagline, String descripcion, String logoUrl, String iconUrl,
            String historia, String mision, String vision) {
        ConfiguracionTienda config = obtenerConfiguracion();
        config.setCorreoMaestro(correo);
        config.setEnviarCopiaMaestro(enviar != null ? enviar : false);
        config.setWhatsappGeneral(whatsapp);
        config.setInstagramUrl(instagram);
        config.setFacebookUrl(facebook);
        config.setTiktokUrl(tiktok);
        config.setImagenHeroUrl(heroUrl);
        config.setImagenBannerUrl(bannerUrl);
        config.setNombreSitio(nombreSitio);
        config.setTagline(tagline);
        config.setDescripcion(descripcion);
        config.setLogoUrl(logoUrl);
        config.setIconUrl(iconUrl);
        config.setHistoria(historia);
        config.setMision(mision);
        config.setVision(vision);
        return configuracionRepository.save(config);
    }
}
