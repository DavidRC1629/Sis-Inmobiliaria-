package com.sisarovi.inmobiliario.repository;

import com.sisarovi.inmobiliario.entity.UserStatus;
import com.sisarovi.inmobiliario.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByDni(String dni);
    boolean existsByDni(String dni);
    List<User> findByEstado(UserStatus estado);
}
