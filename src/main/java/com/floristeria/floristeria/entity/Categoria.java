package com.floristeria.floristeria.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Categorias")
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
}
