package com.sisarovi.inmobiliario.repository;

import com.sisarovi.inmobiliario.entity.UserStatus;
import com.sisarovi.inmobiliario.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByDni(String dni);
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByDni(String dni);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findByEstado(UserStatus estado);
    List<User> findByEstadoAndRejectionExpiresAtAfter(UserStatus estado, LocalDateTime rejectionExpiresAt);
}
