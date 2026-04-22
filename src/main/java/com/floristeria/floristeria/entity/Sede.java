package com.floristeria.floristeria.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Sedes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "ciudad", nullable = false)
    private String ciudad;

    @Column(name = "whatsapp")
    private String whatsapp;

    @Column(name = "email")
    private String email;
}
