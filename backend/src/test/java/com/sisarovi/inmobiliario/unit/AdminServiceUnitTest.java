package com.sisarovi.inmobiliario.unit;

import com.sisarovi.inmobiliario.entity.Role;
import com.sisarovi.inmobiliario.entity.User;
import com.sisarovi.inmobiliario.entity.UserStatus;
import com.sisarovi.inmobiliario.repository.PasswordRecoveryCodeRepository;
import com.sisarovi.inmobiliario.repository.RoleRepository;
import com.sisarovi.inmobiliario.repository.UserRepository;
import com.sisarovi.inmobiliario.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceUnitTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordRecoveryCodeRepository passwordRecoveryCodeRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminService adminService;

    private Role userRole;
    private Role adminRole;
    private User pendingUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        userRole  = buildRole(1L, "ROLE_USER");
        adminRole = buildRole(2L, "ROLE_ADMIN");

        pendingUser = buildUser(10L, "12345678", UserStatus.PENDIENTE, userRole);
        adminUser   = buildUser(1L,  "admin",    UserStatus.ACTIVO,    adminRole);
    }

    // ─── getPendingUsers ────────────────────────────────────────────────────────

    @Test
    void getPendingUsers_returnsList() {
        when(userRepository.findByEstado(UserStatus.PENDIENTE)).thenReturn(List.of(pendingUser));

        List<User> result = adminService.getPendingUsers();

        assertEquals(1, result.size());
        assertEquals(UserStatus.PENDIENTE, result.get(0).getEstado());
    }

    // ─── approveUser ────────────────────────────────────────────────────────────

    @Test
    void approveUser_pendingUser_setsActivo() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(pendingUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = adminService.approveUser(10L);

        assertEquals(UserStatus.ACTIVO, result.getEstado());
        assertNull(result.getRejectionExpiresAt());
    }

    @Test
    void approveUser_notPending_throwsException() {
        User active = buildUser(11L, "99999999", UserStatus.ACTIVO, userRole);
        when(userRepository.findById(11L)).thenReturn(Optional.of(active));

        assertThrows(RuntimeException.class, () -> adminService.approveUser(11L));
        verify(userRepository, never()).save(any());
    }

    @Test
    void approveUser_notFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> adminService.approveUser(99L));
    }

    // ─── rejectUser ─────────────────────────────────────────────────────────────

    @Test
    void rejectUser_pendingUser_setsRechazadoWithCooldown() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(pendingUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = adminService.rejectUser(10L);

        assertEquals(UserStatus.RECHAZADO, result.getEstado());
        assertNotNull(result.getRejectionExpiresAt());
        assertTrue(result.getRejectionExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void rejectUser_notPending_throwsException() {
        User active = buildUser(11L, "99999999", UserStatus.ACTIVO, userRole);
        when(userRepository.findById(11L)).thenReturn(Optional.of(active));

        assertThrows(RuntimeException.class, () -> adminService.rejectUser(11L));
    }

    // ─── cancelRejectionCooldown ────────────────────────────────────────────────

    @Test
    void cancelRejectionCooldown_rejectedUser_clearsExpiry() {
        User rejected = buildUser(12L, "77777777", UserStatus.RECHAZADO, userRole);
        rejected.setRejectionExpiresAt(LocalDateTime.now().plusHours(6));

        when(userRepository.findById(12L)).thenReturn(Optional.of(rejected));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = adminService.cancelRejectionCooldown(12L);

        assertNull(result.getRejectionExpiresAt());
        assertEquals(UserStatus.RECHAZADO, result.getEstado());
    }

    @Test
    void cancelRejectionCooldown_notRejected_throwsException() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(pendingUser));

        assertThrows(RuntimeException.class, () -> adminService.cancelRejectionCooldown(10L));
    }

    // ─── promoteToAdmin ─────────────────────────────────────────────────────────

    @Test
    void promoteToAdmin_existingUser_setsAdminRole() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(pendingUser));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = adminService.promoteToAdmin(10L);

        assertEquals("ROLE_ADMIN", result.getRole().getName());
        assertEquals(UserStatus.ACTIVO, result.getEstado());
        assertTrue(result.isEnabled());
    }

    @Test
    void promoteToAdmin_roleNotFound_throwsException() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(pendingUser));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> adminService.promoteToAdmin(10L));
    }

    // ─── validarPasswordAdmin ───────────────────────────────────────────────────

    @Test
    void validarPasswordAdmin_correctPassword_doesNotThrow() {
        when(userRepository.findByDni("admin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("admin123", adminUser.getPassword())).thenReturn(true);

        assertDoesNotThrow(() -> adminService.validarPasswordAdmin("admin", "admin123"));
    }

    @Test
    void validarPasswordAdmin_wrongPassword_throwsException() {
        when(userRepository.findByDni("admin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("wrong", adminUser.getPassword())).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.validarPasswordAdmin("admin", "wrong"));
        assertEquals("Contraseña de administrador incorrecta", ex.getMessage());
    }

    @Test
    void validarPasswordAdmin_emptyPassword_throwsException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.validarPasswordAdmin("admin", ""));
        assertTrue(ex.getMessage().contains("contraseña"));
    }

    @Test
    void validarPasswordAdmin_nullPassword_throwsException() {
        assertThrows(RuntimeException.class,
                () -> adminService.validarPasswordAdmin("admin", null));
    }

    // ─── deleteUserWithPassword ─────────────────────────────────────────────────

    @Test
    void deleteUserWithPassword_validAdmin_softDeletesUser() {
        User target = buildUser(20L, "55555555", UserStatus.PENDIENTE, userRole);

        when(userRepository.findByDni("admin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("admin123", adminUser.getPassword())).thenReturn(true);
        when(userRepository.findById(20L)).thenReturn(Optional.of(target));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        adminService.deleteUserWithPassword(20L, "admin", "admin123");

        verify(passwordRecoveryCodeRepository).deleteByUser(target);
        verify(userRepository).save(argThat(u ->
                u.getEstado() == UserStatus.RECHAZADO && u.getRejectionExpiresAt() != null));
    }

    @Test
    void deleteUserWithPassword_adminDeletesSelf_throwsException() {
        when(userRepository.findByDni("admin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("admin123", adminUser.getPassword())).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        assertThrows(RuntimeException.class,
                () -> adminService.deleteUserWithPassword(1L, "admin", "admin123"));
    }

    // ─── getAllUsers / getRejectedWaitingUsers ───────────────────────────────────

    @Test
    void getAllUsers_returnsList() {
        when(userRepository.findAll()).thenReturn(List.of(adminUser, pendingUser));

        List<User> result = adminService.getAllUsers();

        assertEquals(2, result.size());
    }

    @Test
    void getRejectedWaitingUsers_returnsWaitingList() {
        User waiting = buildUser(30L, "66666666", UserStatus.RECHAZADO, userRole);
        waiting.setRejectionExpiresAt(LocalDateTime.now().plusHours(3));

        when(userRepository.findByEstadoAndRejectionExpiresAtAfter(eq(UserStatus.RECHAZADO), any()))
                .thenReturn(List.of(waiting));

        List<User> result = adminService.getRejectedWaitingUsers();

        assertEquals(1, result.size());
        assertEquals(UserStatus.RECHAZADO, result.get(0).getEstado());
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    private Role buildRole(Long id, String name) {
        Role role = Role.builder().name(name).build();
        try {
            var f = Role.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(role, id);
        } catch (Exception ignored) {}
        return role;
    }

    private User buildUser(Long id, String dni, UserStatus estado, Role role) {
        User user = User.builder()
                .dni(dni)
                .nombres("Test")
                .primerApellido("User")
                .segundoApellido("")
                .password("encoded")
                .email(dni + "@test.com")
                .role(role)
                .estado(estado)
                .enabled(true)
                .build();
        try {
            var f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(user, id);
        } catch (Exception ignored) {}
        return user;
    }
}
