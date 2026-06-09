package com.sisarovi.inmobiliario.unit;

import com.sisarovi.inmobiliario.exception.GlobalExceptionHandler;
import com.sisarovi.inmobiliario.exception.ReniecServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para GlobalExceptionHandler.
 * Sin Spring context — instanciamos directamente.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ─── BadCredentialsException ────────────────────────────────────────────

    @Test
    void handleBadCredentials_returns401WithCorrectBody() {
        BadCredentialsException ex = new BadCredentialsException("bad creds");

        ResponseEntity<Map<String, String>> response = handler.handleBadCredentials(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DNI o correo o contraseña incorrectos", response.getBody().get("message"));
        assertEquals("INVALID_CREDENTIALS", response.getBody().get("error"));
    }

    // ─── DisabledException ──────────────────────────────────────────────────

    @Test
    void handleDisabled_returns403WithCorrectBody() {
        DisabledException ex = new DisabledException("account disabled");

        ResponseEntity<Map<String, String>> response = handler.handleDisabled(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("ACCOUNT_DISABLED", response.getBody().get("error"));
    }

    // ─── AuthenticationException ────────────────────────────────────────────

    @Test
    void handleAuthenticationException_returns401() {
        AuthenticationException ex = new BadCredentialsException("auth failed");

        ResponseEntity<Map<String, String>> response = handler.handleAuthenticationException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("AUTHENTICATION_FAILED", response.getBody().get("error"));
    }

    // ─── RuntimeException ───────────────────────────────────────────────────

    @Test
    void handleRuntimeException_returns400WithMessage() {
        RuntimeException ex = new RuntimeException("algo salió mal");

        ResponseEntity<Map<String, String>> response = handler.handleRuntimeException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("algo salió mal", response.getBody().get("message"));
        assertEquals("Error", response.getBody().get("error"));
    }

    @Test
    void handleRuntimeException_nullMessage_returns400() {
        RuntimeException ex = new RuntimeException((String) null);

        ResponseEntity<Map<String, String>> response = handler.handleRuntimeException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ─── ReniecServiceUnavailableException ──────────────────────────────────

    @Test
    void handleReniecServiceUnavailable_returns503WithDetail() {
        ReniecServiceUnavailableException ex =
                new ReniecServiceUnavailableException("Servicio no disponible", "timeout");

        ResponseEntity<Map<String, String>> response = handler.handleReniecServiceUnavailable(ex);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("RENIEC_UNAVAILABLE", response.getBody().get("error"));
        assertEquals("timeout", response.getBody().get("detail"));
    }

    // ─── Generic Exception ──────────────────────────────────────────────────

    @Test
    void handleGenericException_returns500() {
        Exception ex = new Exception("unexpected");

        ResponseEntity<Map<String, String>> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Internal Server Error", response.getBody().get("error"));
    }
}
