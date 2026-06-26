package com.floristeria.floristeria.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Inventario")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private Sede sede;

    @Column(name = "precio", nullable = false, precision = 19, scale = 4)
    private BigDecimal precio;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Column(name = "disponible", nullable = false)
    private Boolean disponible;

    @Builder.Default
    @Column(name = "descuento_porcentaje")
    private Integer descuentoPorcentaje = 0;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
