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
@Profile("!test") // <--- ESTO ES LO NUEVO: Se ignora si el perfil es 'test'
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (roleRepository.count() == 0) {
            log.info("Inicializando roles en la base de datos...");

            Role adminRole = Role.builder().id(1L).name("ROLE_ADMIN").build();
            Role userRole = Role.builder().id(2L).name("ROLE_USER").build();

            roleRepository.save(adminRole);
            roleRepository.save(userRole);

            if (userRepository.count() == 0) {
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
            }
        }
    }
}