package com.floristeria.floristeria.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Categorias")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Builder.Default
    @ManyToMany(mappedBy = "categorias", fetch = FetchType.LAZY)
    private List<Producto> productos = new ArrayList<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
