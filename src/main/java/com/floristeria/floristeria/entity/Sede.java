package com.floristeria.floristeria.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "Sedes")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "ciudad", nullable = false)
    private String ciudad;

    @Column(name = "whatsapp")
    private String whatsapp;

    @Column(name = "email")
    private String email;

    @Column(name = "instagram_url")
    private String instagramUrl;

    @Column(name = "facebook_url")
    private String facebookUrl;

    @Column(name = "tiktok_url")
    private String tiktokUrl;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "hora_apertura_entrega")
    @Builder.Default
    private LocalTime horaAperturaEntrega = LocalTime.of(8, 0);

    @Column(name = "hora_cierre_entrega")
    @Builder.Default
    private LocalTime horaCierreEntrega = LocalTime.of(17, 0);

    @Column(name = "hora_corte")
    @Builder.Default
    private LocalTime horaCorte = LocalTime.of(15, 30);

    @Column(name = "ventana_max_dias")
    @Builder.Default
    private Integer ventanaMaxDias = 30;

    @Column(name = "lead_minutos")
    @Builder.Default
    private Integer leadMinutos = 60;

    @Column(name = "dias_no_entrega", length = 64)
    @Builder.Default
    private String diasNoEntrega = "";
}
