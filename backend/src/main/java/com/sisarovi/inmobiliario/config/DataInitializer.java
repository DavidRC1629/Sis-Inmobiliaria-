package com.sisarovi.inmobiliario.config;

import com.sisarovi.inmobiliario.entity.Role;
import com.sisarovi.inmobiliario.entity.User;
import com.sisarovi.inmobiliario.repository.RoleRepository;
import com.sisarovi.inmobiliario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Verificando e inicializando roles en la base de datos...");

        // Buscamos si ya existe el rol ADMIN, si no, lo creamos de forma segura
        Role adminRole = roleRepository.findAll().stream()
                .filter(r -> "ROLE_ADMIN".equals(r.getName()))
                .findFirst()
                .orElseGet(() -> {
                    log.info("Creando ROLE_ADMIN...");
                    return roleRepository.save(Role.builder().name("ROLE_ADMIN").build());
                });

        // Buscamos si ya existe el rol USER, si no, lo creamos de forma segura
        Role userRole = roleRepository.findAll().stream()
                .filter(r -> "ROLE_USER".equals(r.getName()))
                .findFirst()
                .orElseGet(() -> {
                    log.info("Creando ROLE_USER...");
                    return roleRepository.save(Role.builder().name("ROLE_USER").build());
                });

        // Verificamos si ya existe el usuario administrador por su DNI
        boolean adminExists = userRepository.findAll().stream()
                .anyMatch(u -> "admin".equals(u.getDni()));

        if (!adminExists) {
            log.info("Creando usuario administrador por defecto...");
            User adminUser = User.builder()
                    .dni("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .nombres("Admin")
                    .primerApellido("Maestro")
                    .segundoApellido("")
                    .role(adminRole)
                    .estado(com.sisarovi.inmobiliario.entity.UserStatus.ACTIVO)
                    .enabled(true)
                    .build();
            userRepository.save(adminUser);
            log.info("¡Usuario administrador creado con éxito!");
        } else {
            log.info("El usuario administrador ya existe. Omitiendo creación.");
        }
    }
}