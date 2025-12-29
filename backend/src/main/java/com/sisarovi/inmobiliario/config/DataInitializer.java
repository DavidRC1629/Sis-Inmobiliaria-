package com.sisarovi.inmobiliario.config;

import com.sisarovi.inmobiliario.entity.Role;
import com.sisarovi.inmobiliario.entity.User;
import com.sisarovi.inmobiliario.repository.RoleRepository;
import com.sisarovi.inmobiliario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Verificar si la base de datos está vacía
        if (roleRepository.count() == 0) {
            log.info("Inicializando roles en la base de datos...");

            // Crear roles con IDs fijos
            Role adminRole = Role.builder()
                    .id(1L)
                    .name("ROLE_ADMIN")
                    .description("Administrador del sistema con acceso completo")
                    .build();

            Role userRole = Role.builder()
                    .id(2L)
                    .name("ROLE_USER")
                    .description("Usuario estándar con acceso limitado")
                    .build();

            roleRepository.save(adminRole);
            roleRepository.save(userRole);

            log.info("Roles creados: ROLE_ADMIN (ID: 1) y ROLE_USER (ID: 2)");

            // Crear usuario Admin Maestro
            if (userRepository.count() == 0) {
                log.info("Creando usuario 'Admin Maestro'...");

                User adminUser = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .firstName("Admin")
                        .lastName("Maestro")
                        .email("admin@sisarovi.com")
                        .role(adminRole)
                        .enabled(true)
                        .build();

                userRepository.save(adminUser);

                log.info("Usuario Admin Maestro creado exitosamente");
                log.info("Credenciales - Username: admin, Password: admin123");
            }
        } else {
            log.info("La base de datos ya contiene datos. Omitiendo inicialización.");
        }
    }
}
