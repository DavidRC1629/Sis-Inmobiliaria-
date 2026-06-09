package com.sisarovi.inmobiliario.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisarovi.inmobiliario.service.RecoveryEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para RecoveryEmailService.
 * No llama a la API externa Resend.
 */
class RecoveryEmailServiceUnitTest {

    private RecoveryEmailService recoveryEmailService;

    @BeforeEach
    void setUp() {
        recoveryEmailService = new RecoveryEmailService(new ObjectMapper());
    }

    // ─── resendEnabled = false ────────────────────────────────────────────

    @Test
    void sendTemporaryCode_resendDisabled_doesNothing() {
        ReflectionTestUtils.setField(recoveryEmailService, "resendEnabled", false);
        ReflectionTestUtils.setField(recoveryEmailService, "resendApiKey", "");
        ReflectionTestUtils.setField(recoveryEmailService, "resendFromEmail", "onboarding@resend.dev");

        // No debe lanzar excepción
        assertDoesNotThrow(() ->
                recoveryEmailService.sendTemporaryCode("test@test.com", "ABCD1234"));
    }

    // ─── resendEnabled = true, api key vacía ──────────────────────────────

    @Test
    void sendTemporaryCode_noApiKey_throwsRuntimeException() {
        ReflectionTestUtils.setField(recoveryEmailService, "resendEnabled", true);
        ReflectionTestUtils.setField(recoveryEmailService, "resendApiKey", "");
        ReflectionTestUtils.setField(recoveryEmailService, "resendFromEmail", "onboarding@resend.dev");

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                recoveryEmailService.sendTemporaryCode("test@test.com", "ABCD1234"));

        assertTrue(ex.getMessage().contains("API Key"));
    }

    @Test
    void sendTemporaryCode_nullApiKey_throwsRuntimeException() {
        ReflectionTestUtils.setField(recoveryEmailService, "resendEnabled", true);
        ReflectionTestUtils.setField(recoveryEmailService, "resendApiKey", null);
        ReflectionTestUtils.setField(recoveryEmailService, "resendFromEmail", "onboarding@resend.dev");

        assertThrows(RuntimeException.class, () ->
                recoveryEmailService.sendTemporaryCode("test@test.com", "ABCD1234"));
    }
}
