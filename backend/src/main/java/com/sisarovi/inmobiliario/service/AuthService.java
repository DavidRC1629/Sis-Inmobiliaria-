package com.sisarovi.inmobiliario.service;

import com.sisarovi.inmobiliario.dto.AuthResponse;
import com.sisarovi.inmobiliario.dto.LoginRequest;
import com.sisarovi.inmobiliario.dto.RegisterRequest;
import com.sisarovi.inmobiliario.entity.PasswordRecoveryCode;
import com.sisarovi.inmobiliario.entity.Role;
import com.sisarovi.inmobiliario.entity.User;
import com.sisarovi.inmobiliario.entity.UserStatus;
import com.sisarovi.inmobiliario.repository.PasswordRecoveryCodeRepository;
import com.sisarovi.inmobiliario.repository.RoleRepository;
import com.sisarovi.inmobiliario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
        private final PasswordRecoveryCodeRepository passwordRecoveryCodeRepository;
        private final RecoveryEmailService recoveryEmailService;

    public AuthResponse register(RegisterRequest request) {
                                User existingUser = userRepository.findByDni(request.getDni()).orElse(null);

                if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                        throw new RuntimeException("El correo es obligatorio");
                }
                String normalizedEmail = request.getEmail().trim().toLowerCase();

                User existingByEmail = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
                if (existingByEmail != null && (existingUser == null || !existingByEmail.getId().equals(existingUser.getId()))) {
                        throw new RuntimeException("No se puede crear una cuenta con un correo que ya existe");
                }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Rol USER no encontrado"));

                if (existingUser != null) {
                        if (existingUser.getEstado() == UserStatus.ACTIVO) {
                                throw new RuntimeException("El DNI ya está registrado");
                        }

                        if (existingUser.getEstado() == UserStatus.PENDIENTE) {
                                throw new RuntimeException("Ya existe una solicitud pendiente con este DNI");
                        }

                        if (existingUser.getEstado() == UserStatus.RECHAZADO) {
                                LocalDateTime now = LocalDateTime.now();
                                LocalDateTime rejectionExpiresAt = existingUser.getRejectionExpiresAt();

                                if (rejectionExpiresAt != null && rejectionExpiresAt.isAfter(now)) {
                                        long remainingDays = Math.max(1, Duration.between(now, rejectionExpiresAt).toDays());
                                        throw new RuntimeException("Este DNI fue rechazado recientemente. Podrás volver a registrar una solicitud en " + remainingDays + " día(s).");
                                }

                                existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
                                existingUser.setNombres(request.getNombres());
                                existingUser.setPrimerApellido(request.getPrimerApellido());
                                existingUser.setSegundoApellido(request.getSegundoApellido());
                                existingUser.setEmail(normalizedEmail);
                                existingUser.setRole(userRole);
                                existingUser.setEstado(UserStatus.PENDIENTE);
                                existingUser.setEnabled(true);
                                existingUser.setRejectionExpiresAt(null);

                                User savedUser = userRepository.save(existingUser);
                                return buildAuthResponse(savedUser);
                        }
                }

        var user = User.builder()
                .dni(request.getDni())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .nombres(request.getNombres())
                .primerApellido(request.getPrimerApellido())
                .segundoApellido(request.getSegundoApellido())
                .role(userRole)
                .estado(UserStatus.PENDIENTE)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        return buildAuthResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        String identifier = request.getIdentifier() != null ? request.getIdentifier().trim() : "";
        if (identifier.isEmpty()) {
            throw new RuntimeException("Debe ingresar DNI o correo");
        }

        String rawPassword = request.getPassword() != null ? request.getPassword().trim() : "";
        if (rawPassword.isEmpty()) {
            throw new RuntimeException("Debe ingresar la contraseña");
        }

        var user = userRepository.findByDni(identifier)
                .or(() -> userRepository.findByEmailIgnoreCase(identifier))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        boolean loginWithTemporaryCode = false;

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            identifier,
                            rawPassword
                    )
            );
        } catch (AuthenticationException ex) {
            String normalizedCode = rawPassword.toUpperCase();
            PasswordRecoveryCode code = passwordRecoveryCodeRepository
                    .findTopByUserAndCodeAndUsedFalseOrderByCreatedAtDesc(user, normalizedCode)
                    .orElseThrow(() -> new BadCredentialsException("DNI o correo o contraseña incorrectos"));

            if (code.getExpiresAt().isBefore(LocalDateTime.now())) {
                code.setUsed(true);
                passwordRecoveryCodeRepository.save(code);
                throw new RuntimeException("El código temporal expiró. Solicita uno nuevo");
            }

            loginWithTemporaryCode = true;
        }

        // Validar que el usuario tiene estado ACTIVO
        if (user.getEstado() == UserStatus.PENDIENTE) {
            throw new DisabledException("Tu cuenta está en supervisión. Por favor espera a que el administrador apruebe tu solicitud de registro.");
        }

        if (user.getEstado() == UserStatus.RECHAZADO) {
            throw new DisabledException("Tu solicitud de registro ha sido rechazada. No puedes acceder al sistema.");
        }

                return buildAuthResponse(user, loginWithTemporaryCode);
        }

        public AuthResponse getCurrentUserProfile(String identifier) {
                User user = userRepository.findByDni(identifier)
                                .or(() -> userRepository.findByEmailIgnoreCase(identifier))
                                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

                return buildAuthResponse(user);
        }

        private AuthResponse buildAuthResponse(User user) {
                return buildAuthResponse(user, false);
    }

        private AuthResponse buildAuthResponse(User user, boolean requirePasswordChange) {
                var jwtToken = jwtService.generateToken(user, user.getRole().getName());

                return AuthResponse.builder()
                                .token(jwtToken)
                                .dni(user.getDni())
                                .email(user.getEmail())
                                .nombres(user.getNombres())
                                .primerApellido(user.getPrimerApellido())
                                .segundoApellido(user.getSegundoApellido())
                                .role(user.getRole().getName())
                                .message(requirePasswordChange ? "Código temporal válido. Debes cambiar tu contraseña para continuar." : null)
                                .requirePasswordChange(requirePasswordChange)
                                .build();
    }

        public Map<String, String> requestPasswordRecovery(String email) {
                if (email == null || email.trim().isEmpty()) {
                        throw new RuntimeException("Debe ingresar un correo electrónico");
                }

                String normalizedEmail = email.trim().toLowerCase();
                User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                                .orElseThrow(() -> new RuntimeException("El correo ingresado no está registrado en el sistema"));

                invalidatePreviousCodes(user);

                String temporaryCode = generateUniqueTemporaryCode();
                PasswordRecoveryCode code = PasswordRecoveryCode.builder()
                                .user(user)
                                .code(temporaryCode)
                                .expiresAt(LocalDateTime.now().plusMinutes(10))
                                .used(false)
                                .createdAt(LocalDateTime.now())
                                .build();
                passwordRecoveryCodeRepository.save(code);

                recoveryEmailService.sendTemporaryCode(normalizedEmail, temporaryCode);

                Map<String, String> response = new HashMap<>();
                response.put("message", "Te enviamos un código temporal al correo registrado. Válido por 10 minutos.");
                return response;
        }

        public Map<String, String> confirmPasswordRecovery(String email, String temporaryCode, String newPassword) {
                if (email == null || email.trim().isEmpty()) {
                        throw new RuntimeException("Debe ingresar el correo electrónico");
                }
                if (temporaryCode == null || temporaryCode.trim().isEmpty()) {
                        throw new RuntimeException("Debe ingresar el código temporal");
                }
                if (newPassword == null || newPassword.trim().isEmpty()) {
                        throw new RuntimeException("Debe ingresar la nueva contraseña");
                }

                String normalizedEmail = email.trim().toLowerCase();
                String normalizedCode = temporaryCode.trim().toUpperCase();

                User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                                .orElseThrow(() -> new RuntimeException("El correo ingresado no está registrado en el sistema"));

                PasswordRecoveryCode code = passwordRecoveryCodeRepository
                                .findTopByUserAndCodeAndUsedFalseOrderByCreatedAtDesc(user, normalizedCode)
                                .orElseThrow(() -> new RuntimeException("El código temporal es inválido"));

                if (code.getExpiresAt().isBefore(LocalDateTime.now())) {
                        code.setUsed(true);
                        passwordRecoveryCodeRepository.save(code);
                        throw new RuntimeException("El código temporal expiró. Solicita uno nuevo");
                }

                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);

                code.setUsed(true);
                passwordRecoveryCodeRepository.save(code);

                Map<String, String> response = new HashMap<>();
                response.put("message", "Contraseña actualizada correctamente");
                return response;
        }

        private void invalidatePreviousCodes(User user) {
                List<PasswordRecoveryCode> activeCodes = passwordRecoveryCodeRepository.findByUserAndUsedFalse(user);
                for (PasswordRecoveryCode activeCode : activeCodes) {
                        activeCode.setUsed(true);
                }
                if (!activeCodes.isEmpty()) {
                        passwordRecoveryCodeRepository.saveAll(activeCodes);
                }
        }

        private String generateUniqueTemporaryCode() {
                final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
                final SecureRandom random = new SecureRandom();

                for (int attempt = 0; attempt < 20; attempt++) {
                        StringBuilder codeBuilder = new StringBuilder(8);
                        StringBuilder source = new StringBuilder(alphabet);

                        for (int i = 0; i < 8; i++) {
                                int index = random.nextInt(source.length());
                                codeBuilder.append(source.charAt(index));
                                source.deleteCharAt(index);
                        }

                        String candidate = codeBuilder.toString();
                        boolean exists = passwordRecoveryCodeRepository
                                        .existsByCodeAndUsedFalseAndExpiresAtAfter(candidate, LocalDateTime.now());
                        if (!exists) {
                                return candidate;
                        }
                }

                throw new RuntimeException("No se pudo generar un código temporal único. Intenta nuevamente");
        }
}
