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

    public enum CategoriaTipo {
        CATALOGO, ADICIONAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Builder.Default
    @ManyToMany(mappedBy = "categorias", fetch = FetchType.LAZY)
    private List<Producto> productos = new ArrayList<>();

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    @Builder.Default
    private CategoriaTipo tipo = CategoriaTipo.CATALOGO;

    @Column(name = "mostrar_en_catalogo", nullable = false)
    @Builder.Default
    private Boolean mostrarEnCatalogo = true;

    @Column(name = "orden", nullable = false)
    @Builder.Default
    private Integer orden = 0;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
