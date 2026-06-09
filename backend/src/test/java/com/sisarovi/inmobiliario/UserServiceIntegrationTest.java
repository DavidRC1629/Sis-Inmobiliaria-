package com.sisarovi.inmobiliario;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setupRoles() {
        if (roleRepository.findByName("ROLE_USER").isEmpty()) {
            roleRepository.save(Role.builder().name("ROLE_USER").build());
        }
    }

    private User createPendingUser() {
        Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow();
        // DNI max 8 chars (columna varchar(8))
        String shortId = UUID.randomUUID().toString().replace("-", "").substring(0, 7);
        User user = User.builder()
                .dni(shortId)
                .email("u-" + shortId + "@local.test")
                .nombres("User")
                .primerApellido("Service")
                .segundoApellido("Test")
                .password(passwordEncoder.encode("Password123!"))
                .role(userRole)
                .estado(UserStatus.PENDIENTE)
                .enabled(true)
                .build();
        return userRepository.save(user);
    }

    @Test
    void testGetAllUsersAndPendingUsersAndChangeRole() {
        User pending = createPendingUser();
        assertFalse(userService.getAllUsers().isEmpty());

        assertTrue(userService.getPendingUsers().stream()
                .anyMatch(response -> response.getId().equals(pending.getId())));

        Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow();
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").build()));

        ChangeRoleRequest request = new ChangeRoleRequest();
        request.setUserId(pending.getId());
        request.setNewRole(adminRole.getName());

        UserResponse updated = userService.changeUserRole(request);
        assertEquals(adminRole.getName(), updated.getRole());
    }

    @Test
    void testGetUserByIdReturnsCorrectUser() {
        User pending = createPendingUser();
        UserResponse response = userService.getUserById(pending.getId());
        assertEquals(pending.getId(), response.getId());
        assertEquals(pending.getDni(), response.getDni());
    }
}
