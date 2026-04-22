package com.floristeria.floristeria.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Usuarios_Admin")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = true)
    private Sede sede;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "rol", nullable = false)
    private String rol;
}
