package com.floristeria.floristeria.config;

import com.floristeria.floristeria.entity.UsuarioAdmin;
import com.floristeria.floristeria.repository.UsuarioAdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UsuarioAdminRepository usuarioAdminRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (usuarioAdminRepository.count() == 0) {
            UsuarioAdmin admin = UsuarioAdmin.builder()
                    .nombre("Super Administrador")
                    .email("admin@floristeria.com")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .rol("SUPERADMIN")
                    .sede(null)
                    .build();

            usuarioAdminRepository.save(admin);
            System.out.println("Usuario administrador por defecto creado exitosamente.");
        }
    }
}
