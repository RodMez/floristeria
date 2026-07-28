package com.floristeria.floristeria.config;

import com.floristeria.floristeria.entity.UsuarioAdmin;
import com.floristeria.floristeria.repository.ClienteRepository;
import com.floristeria.floristeria.repository.UsuarioAdminRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UsuarioAdminRepository usuarioAdminRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedSuperadmin();
        seedLegacyConsentimiento();
    }

    private void seedSuperadmin() {
        if (usuarioAdminRepository.count() == 0) {
            String adminPassword = environment.getProperty("ADMIN_SEED_PASSWORD");

            if (adminPassword == null || adminPassword.isBlank()) {
                log.error("==========================================================================");
                log.error("NO SE CREÓ EL SUPERADMIN: La variable de entorno ADMIN_SEED_PASSWORD no está configurada.");
                log.error("Debes configurar ADMIN_SEED_PASSWORD en tu archivo .env o variables de entorno");
                log.error("antes del primer arranque. El servidor arrancará sin usuario administrador.");
                log.error("==========================================================================");
                return;
            }

            if (adminPassword.length() < 8) {
                log.error("==========================================================================");
                log.error("NO SE CREÓ EL SUPERADMIN: ADMIN_SEED_PASSWORD debe tener al menos 8 caracteres.");
                log.error("==========================================================================");
                return;
            }

            UsuarioAdmin admin = UsuarioAdmin.builder()
                    .nombre("Super Administrador")
                    .email("admin@floristeria.com")
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .rol("SUPERADMIN")
                    .sede(null)
                    .build();

            usuarioAdminRepository.save(admin);
            log.info("Superadmin creado exitosamente (admin@floristeria.com). Recordá rotar esta contraseña después del primer login.");
        }
    }

    private void seedLegacyConsentimiento() {
        long sinConsentimiento = clienteRepository.countByFechaConsentimientoHabeasIsNull();
        if (sinConsentimiento == 0) {
            return;
        }
        int actualizados = clienteRepository.marcarConsentimientoLegacy("v1-legacy");
        log.warn("==========================================================================");
        log.warn("BACKFILE DE CONSENTIMIENTO: {} clientes existentes recategorizados con version_politica_habeas='v1-legacy'.",
                actualizados);
        log.warn("Esto debe quedar documentado en la Política de Tratamiento como 'transición de recategorización'.");
        log.warn("==========================================================================");
    }
}
