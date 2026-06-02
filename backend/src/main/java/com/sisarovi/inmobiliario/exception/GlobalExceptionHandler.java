package com.sisarovi.inmobiliario.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import com.sisarovi.inmobiliario.exception.ReniecServiceUnavailableException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Invalid credentials attempt");
        
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", "DNI o correo o contraseña incorrectos");
        errorResponse.put("error", "INVALID_CREDENTIALS");
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, String>> handleDisabled(DisabledException ex) {
        log.warn("Disabled account login attempt");
        
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", "Tu cuenta no está habilitada para iniciar sesión. Contacta al administrador.");
        errorResponse.put("error", "ACCOUNT_DISABLED");
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", "DNI o correo o contraseña incorrectos");
        errorResponse.put("error", "AUTHENTICATION_FAILED");
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        log.error("Error: {}", ex.getMessage());
        
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("error", "Error");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ReniecServiceUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleReniecServiceUnavailable(ReniecServiceUnavailableException ex) {
        log.warn("RENIEC unavailable: {}", ex.getDetail());

        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", "Servicio de RENIEC no disponible");
        errorResponse.put("detail", ex.getDetail());
        errorResponse.put("error", "RENIEC_UNAVAILABLE");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Error inesperado: {}", ex.getMessage(), ex);
        
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", "Ha ocurrido un error inesperado");
        errorResponse.put("error", "Internal Server Error");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
