package com.sisarovi.inmobiliario.unit;

import com.sisarovi.inmobiliario.dto.ChangeRoleRequest;
import com.sisarovi.inmobiliario.dto.UserResponse;
import com.sisarovi.inmobiliario.entity.Role;
import com.sisarovi.inmobiliario.entity.User;
import com.sisarovi.inmobiliario.entity.UserStatus;
import com.sisarovi.inmobiliario.repository.RoleRepository;
import com.sisarovi.inmobiliario.repository.UserRepository;
import com.sisarovi.inmobiliario.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    private Role userRole;
    private Role adminRole;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        userRole = Role.builder().name("ROLE_USER").build();
        userRole = setId(userRole, 1L);

        adminRole = Role.builder().name("ROLE_ADMIN").build();
        adminRole = setId(adminRole, 2L);

        sampleUser = User.builder()
                .dni("12345678")
                .nombres("Juan")
                .primerApellido("Perez")
                .segundoApellido("Lopez")
                .password("encoded")
                .email("juan@test.com")
                .role(userRole)
                .estado(UserStatus.ACTIVO)
                .enabled(true)
                .build();
        sampleUser = setUserId(sampleUser, 1L);
    }

    // ─── getAllUsers ────────────────────────────────────────────────────────────

    @Test
    void getAllUsers_returnsMappedList() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));

        List<UserResponse> result = userService.getAllUsers();

        assertEquals(1, result.size());
        assertEquals("12345678", result.get(0).getDni());
        verify(userRepository).findAll();
    }

    @Test
    void getAllUsers_emptyRepo_returnsEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserResponse> result = userService.getAllUsers();

        assertTrue(result.isEmpty());
    }

    // ─── getPendingUsers ────────────────────────────────────────────────────────

    @Test
    void getPendingUsers_returnsOnlyPending() {
        User pending = User.builder()
                .dni("87654321")
                .nombres("Ana")
                .primerApellido("Torres")
                .segundoApellido("")
                .password("encoded")
                .email("ana@test.com")
                .role(userRole)
                .estado(UserStatus.PENDIENTE)
                .enabled(true)
                .build();
        pending = setUserId(pending, 2L);

        when(userRepository.findByEstado(UserStatus.PENDIENTE)).thenReturn(List.of(pending));

        List<UserResponse> result = userService.getPendingUsers();

        assertEquals(1, result.size());
        assertEquals("PENDIENTE", result.get(0).getEstado());
    }

    // ─── getUserById ────────────────────────────────────────────────────────────

    @Test
    void getUserById_existingId_returnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserResponse result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals("12345678", result.getDni());
        assertEquals("Juan", result.getNombres());
    }

    @Test
    void getUserById_nonExistingId_throwsRuntimeException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.getUserById(99L));
        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    // ─── changeUserRole ─────────────────────────────────────────────────────────

    @Test
    void changeUserRole_validRequest_updatesRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ChangeRoleRequest request = new ChangeRoleRequest();
        request.setUserId(1L);
        request.setNewRole("ROLE_ADMIN");

        UserResponse result = userService.changeUserRole(request);

        assertNotNull(result);
        assertEquals("ROLE_ADMIN", result.getRole());
        verify(userRepository).save(sampleUser);
    }

    @Test
    void changeUserRole_userNotFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ChangeRoleRequest request = new ChangeRoleRequest();
        request.setUserId(99L);
        request.setNewRole("ROLE_ADMIN");

        assertThrows(RuntimeException.class, () -> userService.changeUserRole(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changeUserRole_roleNotFound_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(roleRepository.findByName("ROLE_GHOST")).thenReturn(Optional.empty());

        ChangeRoleRequest request = new ChangeRoleRequest();
        request.setUserId(1L);
        request.setNewRole("ROLE_GHOST");

        assertThrows(RuntimeException.class, () -> userService.changeUserRole(request));
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    private Role setId(Role role, Long id) {
        try {
            var field = Role.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(role, id);
        } catch (Exception ignored) {}
        return role;
    }

    private User setUserId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception ignored) {}
        return user;
    }
}
