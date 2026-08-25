package com.floristeria.floristeria.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "configuracion_tienda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionTienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "correo_maestro")
    private String correoMaestro;

    @Column(name = "enviar_copia_maestro", nullable = false)
    @Builder.Default
    private Boolean enviarCopiaMaestro = false;

    @Column(name = "whatsapp_general")
    private String whatsappGeneral;

    @Column(name = "instagram_url")
    private String instagramUrl;

    @Column(name = "facebook_url")
    private String facebookUrl;

    @Column(name = "tiktok_url")
    private String tiktokUrl;

    @Column(name = "imagen_hero_url")
    private String imagenHeroUrl;

    @Column(name = "imagen_banner_url")
    private String imagenBannerUrl;

    @Column(name = "nombre_sitio")
    private String nombreSitio;

    @Column(name = "tagline")
    private String tagline;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "historia", columnDefinition = "TEXT")
    private String historia;

    @Column(name = "historia_imagen_url")
    private String historiaImagenUrl;

    @Column(name = "mision", columnDefinition = "TEXT")
    private String mision;

    @Column(name = "vision", columnDefinition = "TEXT")
    private String vision;

    @Column(name = "showcase_badge")
    private String showcaseBadge;

    @Column(name = "showcase_titulo")
    private String showcaseTitulo;

    @Column(name = "showcase_subtitulo", length = 500)
    private String showcaseSubtitulo;

    @Column(name = "nit")
    private String nit;

    @Column(name = "razon_social")
    private String razonSocial;

    @Column(name = "representante_legal")
    private String representanteLegal;

    @Column(name = "domicilio_comercial")
    private String domicilioComercial;

    @Column(name = "correo_habeas_data")
    private String correoHabeasData;

    @Column(name = "politica_version_actual")
    @Builder.Default
    private String politicaVersionActual = "v1";

    @Column(name = "tyc_version_actual")
    @Builder.Default
    private String tycVersionActual = "v1";
}
