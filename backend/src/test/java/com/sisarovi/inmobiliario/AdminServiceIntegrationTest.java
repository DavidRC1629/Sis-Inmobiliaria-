package com.sisarovi.inmobiliario;

import com.sisarovi.inmobiliario.entity.Role;
import com.sisarovi.inmobiliario.entity.User;
import com.sisarovi.inmobiliario.entity.UserStatus;
import com.sisarovi.inmobiliario.repository.PasswordRecoveryCodeRepository;
import com.sisarovi.inmobiliario.repository.RoleRepository;
import com.sisarovi.inmobiliario.repository.UserRepository;
import com.sisarovi.inmobiliario.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminServiceIntegrationTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void prepareRolesAndAdmin() {
        if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
            roleRepository.save(Role.builder().name("ROLE_ADMIN").build());
        }
        if (roleRepository.findByName("ROLE_USER").isEmpty()) {
            roleRepository.save(Role.builder().name("ROLE_USER").build());
        }
        if (userRepository.findByDni("admin").isEmpty()) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
            userRepository.save(User.builder()
                    .dni("admin")
                    .email("admin@local.test")
                    .nombres("Admin")
                    .primerApellido("Test")
                    .segundoApellido("User")
                    .password(passwordEncoder.encode("admin123"))
                    .role(adminRole)
                    .estado(UserStatus.ACTIVO)
                    .enabled(true)
                    .build());
        }
    }

    private User createPendingUser() {
        Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow();
        // DNI max 8 chars (columna varchar(8))
        String shortId = UUID.randomUUID().toString().replace("-", "").substring(0, 7);
        User user = User.builder()
                .dni(shortId)
                .email("p-" + shortId + "@local.test")
                .nombres("Pending")
                .primerApellido("User")
                .segundoApellido("Test")
                .password(passwordEncoder.encode("Password123!"))
                .role(userRole)
                .estado(UserStatus.PENDIENTE)
                .enabled(true)
                .build();
        return userRepository.save(user);
    }

    @Test
    void testApproveRejectAndCancelRejectionCooldown() {
        User pendingUser = createPendingUser();

        User approved = adminService.approveUser(pendingUser.getId());
        assertEquals(UserStatus.ACTIVO, approved.getEstado());
        assertNull(approved.getRejectionExpiresAt());

        User secondPending = createPendingUser();
        User rejected = adminService.rejectUser(secondPending.getId());
        assertEquals(UserStatus.RECHAZADO, rejected.getEstado());
        assertNotNull(rejected.getRejectionExpiresAt());
        assertTrue(rejected.getRejectionExpiresAt().isAfter(LocalDateTime.now()));

        User cancellation = adminService.cancelRejectionCooldown(rejected.getId());
        assertNull(cancellation.getRejectionExpiresAt());
        assertEquals(UserStatus.RECHAZADO, cancellation.getEstado());
    }

    @Test
    void testPromoteToAdminAndDeleteUserWithPassword() {
        User pendingUser = createPendingUser();
        User promoted = adminService.promoteToAdmin(pendingUser.getId());

        assertEquals("ROLE_ADMIN", promoted.getRole().getName());
        assertTrue(promoted.isEnabled());
        assertEquals(UserStatus.ACTIVO, promoted.getEstado());

        User userToDelete = createPendingUser();
        adminService.deleteUserWithPassword(userToDelete.getId(), "admin", "admin123");

        User deleted = userRepository.findById(userToDelete.getId()).orElseThrow();
        assertEquals(UserStatus.RECHAZADO, deleted.getEstado());
        assertNotNull(deleted.getRejectionExpiresAt());
        assertTrue(deleted.isEnabled());
    }

    @Test
    void testValidatePasswordAdminThrowsWhenWrong() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> adminService.validarPasswordAdmin("admin", "wrong-password"));
        assertEquals("Contraseña de administrador incorrecta", exception.getMessage());
    }
}
