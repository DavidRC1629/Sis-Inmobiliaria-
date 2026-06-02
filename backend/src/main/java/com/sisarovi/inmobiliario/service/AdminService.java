package com.sisarovi.inmobiliario.service;

import com.sisarovi.inmobiliario.entity.User;
import com.sisarovi.inmobiliario.entity.UserStatus;
import com.sisarovi.inmobiliario.entity.Role;
import com.sisarovi.inmobiliario.repository.PasswordRecoveryCodeRepository;
import com.sisarovi.inmobiliario.repository.RoleRepository;
import com.sisarovi.inmobiliario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordRecoveryCodeRepository passwordRecoveryCodeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<User> getPendingUsers() {
        return userRepository.findByEstado(UserStatus.PENDIENTE);
    }

    @Transactional
    public User approveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));
        
        if (user.getEstado() != UserStatus.PENDIENTE) {
            throw new RuntimeException("El usuario no está en estado pendiente");
        }
        
        user.setEstado(UserStatus.ACTIVO);
        user.setRejectionExpiresAt(null);
        return userRepository.save(user);
    }

    @Transactional
    public User rejectUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));
        
        if (user.getEstado() != UserStatus.PENDIENTE) {
            throw new RuntimeException("El usuario no está en estado pendiente");
        }
        
        user.setEstado(UserStatus.RECHAZADO);
        user.setRejectionExpiresAt(LocalDateTime.now().plusDays(5));
        return userRepository.save(user);
    }

    @Transactional
    public User cancelRejectionCooldown(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));

        if (user.getEstado() != UserStatus.RECHAZADO) {
            throw new RuntimeException("El usuario no está rechazado");
        }

        user.setRejectionExpiresAt(null);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> getRejectedWaitingUsers() {
        return userRepository.findByEstadoAndRejectionExpiresAtAfter(UserStatus.RECHAZADO, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User promoteToAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("No se encontró el rol ROLE_ADMIN"));

        user.setRole(adminRole);
        user.setEnabled(true);
        user.setEstado(UserStatus.ACTIVO);
        user.setRejectionExpiresAt(null);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUserWithPassword(Long userId, String adminIdentifier, String rawPassword) {
        validarPasswordAdmin(adminIdentifier, rawPassword);

        User adminUser = userRepository.findByDni(adminIdentifier)
                .or(() -> userRepository.findByEmailIgnoreCase(adminIdentifier))
                .orElseThrow(() -> new RuntimeException("Administrador en sesión no encontrado"));

        User userToDelete = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));

        if (adminUser.getId().equals(userToDelete.getId())) {
            throw new RuntimeException("No puedes eliminar tu propio usuario en sesión");
        }

        try {
            passwordRecoveryCodeRepository.deleteByUser(userToDelete);
            userRepository.delete(userToDelete);
        } catch (DataIntegrityViolationException ex) {
            throw new RuntimeException("No se puede eliminar este usuario porque tiene datos relacionados en el sistema");
        }
    }

    @Transactional(readOnly = true)
    public void validarPasswordAdmin(String adminIdentifier, String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new RuntimeException("Debes ingresar la contraseña del administrador");
        }

        User adminUser = userRepository.findByDni(adminIdentifier)
                .or(() -> userRepository.findByEmailIgnoreCase(adminIdentifier))
                .orElseThrow(() -> new RuntimeException("Administrador en sesión no encontrado"));

        if (!passwordEncoder.matches(rawPassword, adminUser.getPassword())) {
            throw new RuntimeException("Contraseña de administrador incorrecta");
        }
    }
}
