package com.floristeria.floristeria.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "Direcciones")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Direccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "alias", nullable = false)
    private String alias;

    @Column(name = "direccion", nullable = false)
    private String direccion;

    @Column(name = "ciudad", nullable = false)
    private String ciudad;

    @Column(name = "detalles")
    private String detalles;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_domicilio_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private ZonaDomicilio zonaDomicilio;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
