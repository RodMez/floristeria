package com.floristeria.floristeria.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Inventario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // --- AQUÍ ESTÁ LA MAGIA JPA ---
    // En lugar de un Integer, mapeamos la entidad completa
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;
    // ------------------------------

    @Column(name = "precio", nullable = false)
    private Double precio;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Column(name = "disponible", nullable = false)
    private Boolean disponible;
}