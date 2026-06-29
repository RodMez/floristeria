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
}
