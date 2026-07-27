package com.floristeria.floristeria.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "Clientes")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "fecha_consentimiento_habeas")
    private LocalDateTime fechaConsentimientoHabeas;

    @Column(name = "version_politica_habeas")
    @Builder.Default
    private String versionPoliticaHabeas = "v1";

    @Column(name = "fecha_solicitud_supresion")
    private LocalDateTime fechaSolicitudSupresion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_supresion")
    @Builder.Default
    private EstadoSupresion estadoSupresion = EstadoSupresion.NINGUNA;

    @PrePersist
    @PreUpdate
    protected void onCreate() {
        if (this.creadoEn == null) {
            this.creadoEn = LocalDateTime.now();
        }
        if (this.email != null) {
            this.email = this.email.toLowerCase().trim();
        }
    }
}
