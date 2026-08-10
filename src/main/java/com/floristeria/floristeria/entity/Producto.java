package com.floristeria.floristeria.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "Productos")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "producto_categoria",
            joinColumns = @JoinColumn(name = "producto_id"),
            inverseJoinColumns = @JoinColumn(name = "categoria_id"))
    private List<Categoria> categorias = new ArrayList<>();

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "descripcion", length = 1500)
    private String descripcion;

    @Column(name = "imagen_url")
    private String imagenUrl;

    @Column(unique = true)
    private String sku;

    @Column(name = "activo_global", nullable = false)
    private Boolean activoGlobal;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        if (this.creadoEn == null) {
            this.creadoEn = LocalDateTime.now();
        }
    }
}
