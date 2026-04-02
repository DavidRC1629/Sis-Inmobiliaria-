package com.sisarovi.inmobiliario.service;

import com.sisarovi.inmobiliario.entity.User;
import com.sisarovi.inmobiliario.entity.UserStatus;
import com.sisarovi.inmobiliario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

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
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
