package com.sisarovi.inmobiliario.repository;

import com.sisarovi.inmobiliario.entity.PasswordRecoveryCode;
import com.sisarovi.inmobiliario.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordRecoveryCodeRepository extends JpaRepository<PasswordRecoveryCode, Long> {
    boolean existsByCodeAndUsedFalseAndExpiresAtAfter(String code, LocalDateTime now);

    List<PasswordRecoveryCode> findByUserAndUsedFalse(User user);

    void deleteByUser(User user);

    Optional<PasswordRecoveryCode> findTopByUserAndCodeAndUsedFalseOrderByCreatedAtDesc(User user, String code);
}
