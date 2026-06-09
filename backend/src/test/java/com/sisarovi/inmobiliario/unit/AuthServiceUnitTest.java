package com.sisarovi.inmobiliario.unit;

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
import com.sisarovi.inmobiliario.service.AuthService;
import com.sisarovi.inmobiliario.service.JwtService;
import com.sisarovi.inmobiliario.service.RecoveryEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceUnitTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private PasswordRecoveryCodeRepository passwordRecoveryCodeRepository;
    @Mock private RecoveryEmailService recoveryEmailService;

    @InjectMocks private AuthService authService;

    private Role userRole;
    private Role adminRole;
    private User activeUser;

    @BeforeEach
    void setUp() {
        userRole  = buildRole(1L, "ROLE_USER");
        adminRole = buildRole(2L, "ROLE_ADMIN");

        activeUser = buildUser(1L, "12345678", "user@test.com", UserStatus.ACTIVO, adminRole);

        when(jwtService.generateToken(any(User.class), anyString())).thenReturn("mock-jwt-token");
    }

    // ════════════════════════════════════════════════════════════════════════
    // register
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void register_newUser_savesAndReturnsToken() {
        when(userRepository.findByDni("11111111")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("new@test.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            setField(u, "id", 99L);
            return u;
        });

        AuthResponse result = authService.register(buildRegisterRequest("11111111", "new@test.com"));

        assertNotNull(result.getToken());
        assertEquals("11111111", result.getDni());
    }

    @Test
    void register_nullEmail_throws() {
        RegisterRequest req = buildRegisterRequest("11111111", null);
        assertThrows(RuntimeException.class, () -> authService.register(req));
    }

    @Test
    void register_emptyEmail_throws() {
        RegisterRequest req = buildRegisterRequest("11111111", "  ");
        assertThrows(RuntimeException.class, () -> authService.register(req));
    }

    @Test
    void register_emailAlreadyUsedByDifferentUser_throws() {
        User other = buildUser(50L, "99999999", "taken@test.com", UserStatus.ACTIVO, userRole);
        when(userRepository.findByDni("11111111")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("taken@test.com")).thenReturn(Optional.of(other));

        assertThrows(RuntimeException.class,
                () -> authService.register(buildRegisterRequest("11111111", "taken@test.com")));
    }

    @Test
    void register_existingActiveUser_throws() {
        User active = buildUser(2L, "11111111", "x@test.com", UserStatus.ACTIVO, userRole);
        when(userRepository.findByDni("11111111")).thenReturn(Optional.of(active));
        when(userRepository.findByEmailIgnoreCase("x@test.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));

        assertThrows(RuntimeException.class,
                () -> authService.register(buildRegisterRequest("11111111", "x@test.com")));
    }

    @Test
    void register_existingPendingUser_throws() {
        User pending = buildUser(3L, "11111111", "p@test.com", UserStatus.PENDIENTE, userRole);
        when(userRepository.findByDni("11111111")).thenReturn(Optional.of(pending));
        when(userRepository.findByEmailIgnoreCase("p@test.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));

        assertThrows(RuntimeException.class,
                () -> authService.register(buildRegisterRequest("11111111", "p@test.com")));
    }

    @Test
    void register_existingRejectedUserInCooldown_throws() {
        User rejected = buildUser(4L, "11111111", "r@test.com", UserStatus.RECHAZADO, userRole);
        rejected.setRejectionExpiresAt(LocalDateTime.now().plusHours(6));
        when(userRepository.findByDni("11111111")).thenReturn(Optional.of(rejected));
        when(userRepository.findByEmailIgnoreCase("r@test.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register(buildRegisterRequest("11111111", "r@test.com")));
        assertTrue(ex.getMessage().contains("hora"));
    }

    @Test
    void register_existingRejectedUserCooldownExpired_reregisters() {
        User rejected = buildUser(4L, "11111111", "r@test.com", UserStatus.RECHAZADO, userRole);
        rejected.setRejectionExpiresAt(LocalDateTime.now().minusHours(1)); // expirado
        when(userRepository.findByDni("11111111")).thenReturn(Optional.of(rejected));
        when(userRepository.findByEmailIgnoreCase("r@test.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse result = authService.register(buildRegisterRequest("11111111", "r@test.com"));

        assertNotNull(result);
        verify(userRepository).save(rejected);
    }

    @Test
    void register_roleNotFound_throws() {
        when(userRepository.findByDni("11111111")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("n@test.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> authService.register(buildRegisterRequest("11111111", "n@test.com")));
    }

    // ════════════════════════════════════════════════════════════════════════
    // login
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void login_validCredentials_returnsToken() {
        LoginRequest req = new LoginRequest("12345678", "pass");
        when(userRepository.findByDni("12345678")).thenReturn(Optional.of(activeUser));
        // authenticationManager.authenticate no hace nada por defecto (void method)

        AuthResponse result = authService.login(req);

        assertNotNull(result.getToken());
        assertEquals("12345678", result.getDni());
    }

    @Test
    void login_emptyIdentifier_throws() {
        LoginRequest req = new LoginRequest("", "pass");
        assertThrows(RuntimeException.class, () -> authService.login(req));
    }

    @Test
    void login_emptyPassword_throws() {
        LoginRequest req = new LoginRequest("12345678", "");
        assertThrows(RuntimeException.class, () -> authService.login(req));
    }

    @Test
    void login_userNotFound_throws() {
        LoginRequest req = new LoginRequest("ghost", "pass");
        when(userRepository.findByDni("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("ghost")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.login(req));
    }

    @Test
    void login_pendingUser_throwsDisabled() {
        User pending = buildUser(5L, "55555555", "p@test.com", UserStatus.PENDIENTE, userRole);
        LoginRequest req = new LoginRequest("55555555", "pass");
        when(userRepository.findByDni("55555555")).thenReturn(Optional.of(pending));
        // authenticate pasa OK (void, no throws por defecto)

        assertThrows(DisabledException.class, () -> authService.login(req));
    }

    @Test
    void login_rejectedUser_throwsDisabled() {
        User rejected = buildUser(6L, "66666666", "r@test.com", UserStatus.RECHAZADO, userRole);
        LoginRequest req = new LoginRequest("66666666", "pass");
        when(userRepository.findByDni("66666666")).thenReturn(Optional.of(rejected));
        // authenticate pasa OK (void, no throws por defecto)

        assertThrows(DisabledException.class, () -> authService.login(req));
    }

    @Test
    void login_badCredentials_noRecoveryCode_throwsBadCredentials() {
        LoginRequest req = new LoginRequest("12345678", "wrongpass");
        when(userRepository.findByDni("12345678")).thenReturn(Optional.of(activeUser));
        doThrow(new BadCredentialsException("bad")).when(authenticationManager).authenticate(any());
        when(passwordRecoveryCodeRepository
                .findTopByUserAndCodeAndUsedFalseOrderByCreatedAtDesc(eq(activeUser), any()))
                .thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }

    @Test
    void login_expiredTemporaryCode_throws() {
        LoginRequest req = new LoginRequest("12345678", "ABCDEFGH");
        when(userRepository.findByDni("12345678")).thenReturn(Optional.of(activeUser));
        doThrow(new BadCredentialsException("bad")).when(authenticationManager).authenticate(any());

        PasswordRecoveryCode expiredCode = PasswordRecoveryCode.builder()
                .user(activeUser)
                .code("ABCDEFGH")
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .used(false)
                .build();
        setField(expiredCode, "id", 1L);

        when(passwordRecoveryCodeRepository
                .findTopByUserAndCodeAndUsedFalseOrderByCreatedAtDesc(eq(activeUser), eq("ABCDEFGH")))
                .thenReturn(Optional.of(expiredCode));
        when(passwordRecoveryCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(RuntimeException.class, () -> authService.login(req));
        assertTrue(expiredCode.isUsed());
    }

    @Test
    void login_validTemporaryCode_returnsRequirePasswordChange() {
        LoginRequest req = new LoginRequest("12345678", "VALIDCOD");
        when(userRepository.findByDni("12345678")).thenReturn(Optional.of(activeUser));
        doThrow(new BadCredentialsException("bad")).when(authenticationManager).authenticate(any());

        PasswordRecoveryCode validCode = PasswordRecoveryCode.builder()
                .user(activeUser)
                .code("VALIDCOD")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();
        setField(validCode, "id", 2L);

        when(passwordRecoveryCodeRepository
                .findTopByUserAndCodeAndUsedFalseOrderByCreatedAtDesc(eq(activeUser), eq("VALIDCOD")))
                .thenReturn(Optional.of(validCode));

        AuthResponse result = authService.login(req);

        assertTrue(Boolean.TRUE.equals(result.getRequirePasswordChange()));
    }

    // ════════════════════════════════════════════════════════════════════════
    // getCurrentUserProfile
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void getCurrentUserProfile_byDni_returnsResponse() {
        when(userRepository.findByDni("12345678")).thenReturn(Optional.of(activeUser));

        AuthResponse result = authService.getCurrentUserProfile("12345678");

        assertEquals("12345678", result.getDni());
    }

    @Test
    void getCurrentUserProfile_notFound_throws() {
        when(userRepository.findByDni("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("ghost")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.getCurrentUserProfile("ghost"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // requestPasswordRecovery
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void requestPasswordRecovery_nullEmail_throws() {
        assertThrows(RuntimeException.class, () -> authService.requestPasswordRecovery(null));
    }

    @Test
    void requestPasswordRecovery_emptyEmail_throws() {
        assertThrows(RuntimeException.class, () -> authService.requestPasswordRecovery("  "));
    }

    @Test
    void requestPasswordRecovery_emailNotFound_throws() {
        when(userRepository.findByEmailIgnoreCase("nope@test.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.requestPasswordRecovery("nope@test.com"));
    }

    @Test
    void requestPasswordRecovery_validEmail_sendsCode() {
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(activeUser));
        when(passwordRecoveryCodeRepository.findByUserAndUsedFalse(activeUser)).thenReturn(List.of());
        when(passwordRecoveryCodeRepository.existsByCodeAndUsedFalseAndExpiresAtAfter(any(), any())).thenReturn(false);
        when(passwordRecoveryCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(recoveryEmailService).sendTemporaryCode(any(), any());

        Map<String, String> result = authService.requestPasswordRecovery("user@test.com");

        assertNotNull(result.get("message"));
        verify(recoveryEmailService).sendTemporaryCode(eq("user@test.com"), any());
    }

    @Test
    void requestPasswordRecovery_invalidatesPreviousCodes() {
        PasswordRecoveryCode old = PasswordRecoveryCode.builder()
                .user(activeUser).code("OLD").used(false)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        setField(old, "id", 10L);

        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(activeUser));
        when(passwordRecoveryCodeRepository.findByUserAndUsedFalse(activeUser)).thenReturn(List.of(old));
        when(passwordRecoveryCodeRepository.saveAll(any())).thenReturn(List.of(old));
        when(passwordRecoveryCodeRepository.existsByCodeAndUsedFalseAndExpiresAtAfter(any(), any())).thenReturn(false);
        when(passwordRecoveryCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(recoveryEmailService).sendTemporaryCode(any(), any());

        authService.requestPasswordRecovery("user@test.com");

        assertTrue(old.isUsed());
        verify(passwordRecoveryCodeRepository).saveAll(any());
    }

    // ════════════════════════════════════════════════════════════════════════
    // confirmPasswordRecovery
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void confirmPasswordRecovery_nullEmail_throws() {
        assertThrows(RuntimeException.class,
                () -> authService.confirmPasswordRecovery(null, "CODE", "newpass"));
    }

    @Test
    void confirmPasswordRecovery_nullCode_throws() {
        assertThrows(RuntimeException.class,
                () -> authService.confirmPasswordRecovery("e@t.com", null, "newpass"));
    }

    @Test
    void confirmPasswordRecovery_nullPassword_throws() {
        assertThrows(RuntimeException.class,
                () -> authService.confirmPasswordRecovery("e@t.com", "CODE", null));
    }

    @Test
    void confirmPasswordRecovery_invalidCode_throws() {
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(activeUser));
        when(passwordRecoveryCodeRepository
                .findTopByUserAndCodeAndUsedFalseOrderByCreatedAtDesc(activeUser, "BADCODE"))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> authService.confirmPasswordRecovery("user@test.com", "BADCODE", "newpass"));
    }

    @Test
    void confirmPasswordRecovery_expiredCode_throws() {
        PasswordRecoveryCode expired = PasswordRecoveryCode.builder()
                .user(activeUser).code("EXPIRED")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .used(false).build();
        setField(expired, "id", 5L);

        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(activeUser));
        when(passwordRecoveryCodeRepository
                .findTopByUserAndCodeAndUsedFalseOrderByCreatedAtDesc(activeUser, "EXPIRED"))
                .thenReturn(Optional.of(expired));
        when(passwordRecoveryCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(RuntimeException.class,
                () -> authService.confirmPasswordRecovery("user@test.com", "EXPIRED", "newpass"));
        assertTrue(expired.isUsed());
    }

    @Test
    void confirmPasswordRecovery_validCode_updatesPassword() {
        PasswordRecoveryCode valid = PasswordRecoveryCode.builder()
                .user(activeUser).code("VALIDCOD")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false).build();
        setField(valid, "id", 6L);

        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(activeUser));
        when(passwordRecoveryCodeRepository
                .findTopByUserAndCodeAndUsedFalseOrderByCreatedAtDesc(activeUser, "VALIDCOD"))
                .thenReturn(Optional.of(valid));
        when(passwordEncoder.encode("newpass")).thenReturn("encoded-new");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(passwordRecoveryCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, String> result = authService.confirmPasswordRecovery("user@test.com", "VALIDCOD", "newpass");

        assertEquals("Contraseña actualizada correctamente", result.get("message"));
        assertTrue(valid.isUsed());
        verify(userRepository).save(activeUser);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Role buildRole(Long id, String name) {
        Role r = Role.builder().name(name).build();
        setField(r, "id", id);
        return r;
    }

    private User buildUser(Long id, String dni, String email, UserStatus estado, Role role) {
        User u = User.builder()
                .dni(dni).nombres("Test").primerApellido("U").segundoApellido("")
                .password("pw").email(email).role(role).estado(estado).enabled(true).build();
        setField(u, "id", id);
        return u;
    }

    private RegisterRequest buildRegisterRequest(String dni, String email) {
        return RegisterRequest.builder()
                .dni(dni).email(email).password("pass")
                .nombres("Test").primerApellido("User").segundoApellido("X").build();
    }

    private void setField(Object obj, String name, Object value) {
        try {
            var f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception ignored) {}
    }
}
