package com.sisarovi.inmobiliario.service;

import com.sisarovi.inmobiliario.dto.ChangeRoleRequest;
import com.sisarovi.inmobiliario.dto.UserResponse;
import com.sisarovi.inmobiliario.entity.Role;
import com.sisarovi.inmobiliario.entity.User;
import com.sisarovi.inmobiliario.repository.RoleRepository;
import com.sisarovi.inmobiliario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse changeUserRole(ChangeRoleRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Role newRole = roleRepository.findByName(request.getNewRole())
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + request.getNewRole()));

        user.setRole(newRole);
        User updatedUser = userRepository.save(user);

        return mapToUserResponse(updatedUser);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .dni(user.getDni())
                .nombres(user.getNombres())
                .primerApellido(user.getPrimerApellido())
                .segundoApellido(user.getSegundoApellido())
                .role(user.getRole().getName())
                .estado(user.getEstado() != null ? user.getEstado().name() : null)
                .enabled(user.isEnabled())
                .build();
    }
}
