package com.sisarovi.inmobiliario.service;

import com.sisarovi.inmobiliario.dto.AuthResponse;
import com.sisarovi.inmobiliario.dto.LoginRequest;
import com.sisarovi.inmobiliario.dto.RegisterRequest;
import com.sisarovi.inmobiliario.entity.Role;
import com.sisarovi.inmobiliario.entity.UserStatus;
import com.sisarovi.inmobiliario.entity.User;
import com.sisarovi.inmobiliario.repository.RoleRepository;
import com.sisarovi.inmobiliario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByDni(request.getDni())) {
            throw new RuntimeException("El DNI ya está registrado");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Rol USER no encontrado"));

        var user = User.builder()
                .dni(request.getDni())
                .password(passwordEncoder.encode(request.getPassword()))
                .nombres(request.getNombres())
                .primerApellido(request.getPrimerApellido())
                .segundoApellido(request.getSegundoApellido())
                .role(userRole)
                .estado(UserStatus.PENDIENTE)
                .enabled(true)
                .build();

        userRepository.save(user);

        var jwtToken = jwtService.generateToken(user, user.getRole().getName());

        return AuthResponse.builder()
                .token(jwtToken)
                .dni(user.getDni())
                .nombres(user.getNombres())
                .primerApellido(user.getPrimerApellido())
                .segundoApellido(user.getSegundoApellido())
                .role(user.getRole().getName())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getDni(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByDni(request.getDni())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        var jwtToken = jwtService.generateToken(user, user.getRole().getName());

        return AuthResponse.builder()
                .token(jwtToken)
                .dni(user.getDni())
                .nombres(user.getNombres())
                .primerApellido(user.getPrimerApellido())
                .segundoApellido(user.getSegundoApellido())
                .role(user.getRole().getName())
                .build();
    }
}
