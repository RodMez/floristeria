package com.floristeria.floristeria.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Zonas_Domicilio")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZonaDomicilio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private Sede sede;

    @Column(name = "localidad", nullable = false)
    private String localidad;

    @Column(name = "barrio")
    private String barrio;

    @Column(name = "precio", nullable = false)
    private BigDecimal precio;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
